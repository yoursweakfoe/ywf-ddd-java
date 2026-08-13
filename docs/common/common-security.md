# common-security

零信任身份 —— JWT 资源服务器（服务自验 JWT、身份不传播、验签可插拔、claim 可配置）。

> 本文分两段：§1–4 面向使用者（怎么用），§5–7 面向设计者（为什么这么设计）。

## 1. 定位与边界

在零信任 / 防御纵深架构下，为微服务提供 JWT 身份验签与解析能力。每个服务作为 OAuth2 资源服务器（resource server），自行验签 JWT，**不信任网络里的任何身份 Header**。

> 本模块抽象「机制」，把「字段 / 密钥方案」做成可插拔：验签（`JwtDecoder` 可换、可分发）、身份字段（claim 名可配置）、角色映射（claim 可配置）。不把公司的混乱现状（非标 claim、多密钥方案）焊死进框架。

## 2. 核心能力

### 身份流转（南北向 + 东西向）

```
客户端 → 认证服务（签发 JWT）
       → Higress 网关（PEP：转发 JWT，不注入身份 Header）
       → 服务（BearerTokenAuthenticationFilter + JwtDecoder 自验签 → CurrentUser）
       └ 东西向 → Feign（RequestInterceptor 透传同一 JWT）→ 下游服务自验签
```

### 身份模型

验签后，`CurrentUserJwtAuthenticationConverter` 把 JWT 映射为类型化身份 `CurrentUser`：

| 字段 | 类型 | 来源 claim（可配置） |
|------|------|--------------------|
| `userId` | String | `sub`（默认，标准） |
| `username` | String | `uname`（默认，公司现状） |
| `roles` | List\<String\> | `roles`（默认） |

- claim 名经 `SecurityProperties` 配置（见 §3），字段类型统一归一——数值型 `uid`、非标 `uname` 都收敛为字符串 / 列表。
- principal 是 `CurrentUser`（`@AuthenticationPrincipal CurrentUser` 直接注入）；原始 `Jwt` 保留在 credentials（`SecurityUtil.getJwt()` 取出，供东西向透传）。

### 多验签方案（不同来源不同算法）

`DelegatingJwtDecoder` 按 JOSE 头 `alg` 分发到对应 `JwtDecoder`：

```java
@Bean
JwtDecoder jwtDecoder() {
    return new DelegatingJwtDecoder(Map.of(
        "HS256", NimbusJwtDecoder.withSecretKey(hmacKey).macAlgorithm(HS256).build(),
        "RS256", NimbusJwtDecoder.withJwkSetUri(jwksUrl).build()
    ));
}
```

### SecurityUtil

| 方法 | 未登录时返回 |
|------|------------|
| `getCurrentUser()` | `null` |
| `getCurrentUserId()` | `null` |
| `getCurrentUsername()` | `null` |
| `getCurrentRoles()` | 空 List |
| `getJwt()` | `null` |

### 自动装配

`SecurityAutoConfiguration` 注册：

| Bean | 条件 | 说明 |
|------|------|------|
| `CurrentUserJwtAuthenticationConverter` | 无条件 | JWT → `CurrentUser` + `ROLE_*` 权限（claim 名可配置） |
| 资源服务器 `SecurityFilterChain` | Servlet Web 应用 + 无自定义链 | `oauth2ResourceServer().jwt()` + CSRF 关闭 + 无状态 + permit-all |
| `@EnableMethodSecurity` | 无条件 | 启用 `@PreAuthorize` / `@Secured` |

## 3. 使用方式

```xml
<dependency>
    <groupId>com.yoursweakfoe</groupId>
    <artifactId>common-security</artifactId>
</dependency>
```

### 配置：JwtDecoder（必配）

- **非对称（JWKS）**：
  ```yaml
  spring:
    security:
      oauth2:
        resourceserver:
          jwt:
            jwk-set-uri: https://${IDP_HOST}/.well-known/jwks.json
  ```
- **对称密钥（HMAC，公司现状）**：自定义 Bean
  ```java
  @Bean
  JwtDecoder jwtDecoder() {
      SecretKey key = new SecretKeySpec(Base64.getDecoder().decode(secret), "HmacSHA256");
      return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
  }
  ```
- **多方案**：见 §2 的 `DelegatingJwtDecoder`。

### 配置：claim 名（可选，默认对齐公司现状）

```yaml
ywf:
  security:
    user-id-claim: uid      # 默认 sub；公司数值 uid 场景改 uid
    username-claim: uname   # 默认 uname
    roles-claim: roles      # 默认 roles
```

### 场景 1：获取当前用户身份

```java
// Controller：注入类型化身份
@GetMapping("/orders/{id}")
OrderCO get(@AuthenticationPrincipal CurrentUser user, @PathVariable String id) {
    return orderAppService.getOrder(id, user == null ? null : user.userId());
}

// Application / Adapter 层：SecurityUtil 静态访问
String userId = SecurityUtil.getCurrentUserId();   // 字符串，数值 uid 已归一
Order order = new Order(command, userId);          // 审计字段、数据归属
```

### 场景 2：方法级鉴权

```java
@PreAuthorize("hasRole('ADMIN')")
public void approve(OrderCommand command) { ... }

// 数据归属判断（域数据依赖，无法上浮网关）
boolean isOwner = order.getCustomerId().equals(SecurityUtil.getCurrentUserId());
boolean isAdmin = SecurityUtil.getCurrentRoles().contains("ADMIN");
```

### 场景 3：自定义安全链

```java
@Configuration
@EnableWebSecurity
class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, CurrentUserJwtAuthenticationConverter converter) {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter)))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().permitAll());
        return http.build();
    }
}
```

## 4. 依赖关系

```
common-security → spring-boot-starter-security
                → spring-boot-starter-security-oauth2-resource-server（JwtDecoder + BearerTokenAuthenticationFilter + jose）
                → spring-boot-autoconfigure
```

## 5. 设计原则

- **零信任 / 防御纵深**：服务自验 JWT，不信任网络身份 Header；网关只是 PEP，转发 JWT 而非身份断言
- **机制与字段分离**：框架抽象「验签 / 建身份 / 无状态链 / 方法级鉴权」；「字段命名 / 密钥方案」做成可配置、可插拔，不焊死公司现状
- **身份即 CurrentUser，JWT 留底**：principal 是类型化 `CurrentUser`，原始 `Jwt` 存 credentials 供东西向透传
- **fail-closed**：坏 token → 401，绝不静默放行（对比旧框架的 `catch (Exception ignored)`）
- **边界 permit-all + 方法级鉴权**：路由级在网关，服务用 `@PreAuthorize` 做细粒度鉴权

## 6. 设计决策

### ADR-0001 网关验签 + 服务信任 Header

- 状态：superseded by ADR-0005

**原决策**：验签由网关完成，服务只信任网关透传的 Header（`X-User-Id` 等）。

**废弃原因**：企业推动零信任，该模式违反「不信任网络、每跳验证」，Header 可伪造。

### ADR-0002 身份来源标记（EDGE）

- 状态：superseded by ADR-0005

**原决策**：`IdentitySource` 标记身份来源。**废弃原因**：零信任下身份一律来自自验 JWT，「来源」恒为 JWT。

### ADR-0003 边界 permit-all SecurityFilterChain

- 状态：accepted

**决策**：路由级鉴权在网关；服务层提供 permit-all + 无状态链，可被服务自定义链覆盖；细粒度鉴权用 `@PreAuthorize`。

### ADR-0004 预认证 + 链内注册（Header 解析过滤器）

- 状态：superseded by ADR-0005

**废弃原因**：Header 透传本身在零信任下废弃。

### ADR-0005 零信任：服务自验 JWT（资源服务器）

- 状态：accepted

**决策**：服务下沉为 OAuth2 资源服务器，自行验签 JWT。网关降为 PEP，转发 JWT 本身、不注入身份 Header。

### ADR-0006 claim 可配置（不硬编码字段）

- 状态：accepted

**背景**：公司 JWT 的 claim 命名无规范（`uid`/`uname` 非标准）、字段随版本变化。

**决策**：claim 名经 `SecurityProperties` 配置（默认 `sub`/`uname`/`roles`），字段类型由转换器统一归一为字符串 / 列表。原始 `Jwt` 始终经 `getJwt()` 可取。

### ADR-0007 验签可插拔：JwtDecoder 抽象 + 多方案分发

- 状态：accepted

**背景**：不同来源的 JWT 使用不同签名算法（HS256 / RS256 …），密钥方案未统一。

**决策**：`JwtDecoder` 接口即抽象（框架只认 `decode(token)`）；`DelegatingJwtDecoder` 按 JOSE 头 `alg` 分发到各方案 decoder。可选工具类，不自动装配，谁需要谁 `new`。

## 7. 职责边界与技术债

| 项 | 说明 |
|---|---|
| 边界：JWT 签发 / 刷新 / 登出 | 由独立认证服务（IdP）处理，**服务侧不提供签发能力**（旧框架的 `generateToken` 下发到服务是反例） |
| 边界：路由级鉴权 | 由网关（PEP）处理 |
| 边界：RBAC 权限模型 | 角色/权限管理属于业务域，各服务按需实现 |
| 边界：数据权限（行级过滤） | 与业务模型强耦合，由业务层 SQL 条件自行实现 |
| 边界：机器身份（client-credentials） | 定时任务 / MQ 等无用户上下文的调用，需走 client-credentials 取 token，另行设计 |
| 演进：token 交换 / 受众限制 | 东西向透传同一 JWT 为基线；按需引入 RFC 8693 token exchange 或 audience/scope 限制 |
| 风险：HMAC 共享密钥 | 对称密钥意味着「验签方 = 签收方」，任一服务被攻破即可伪造 token；长远可迁非对称（JWKS） |
