# common-security

零信任身份 —— JWT 资源服务器（服务自验 JWT、身份不传播、验签可插拔、字段不写死）。

> 本文分两段：§1–4 面向使用者（怎么用），§5–7 面向设计者（为什么这么设计）。

## 1. 定位与边界

在零信任 / 防御纵深架构下，为微服务提供 JWT 身份验签与解析能力。每个服务作为 OAuth2 资源服务器（resource server），自行验签 JWT，**不信任网络里的任何身份 Header**。

> 本模块抽象「机制」，把「密钥方案」「字段命名/数量」做成可插拔 / 按需读取：验签（`JwtDecoder` 可换、可分发）、身份字段（不投影成固定结构，按名字自取）。不把公司的混乱现状（非标 claim、多密钥方案、字段数量不定）焊死进框架。

## 2. 核心能力

### 包结构

模块根包 `com.yoursweakfoe.common.security` 只放自动装配（与其它 common 模块约定一致），其余内容按话题分文件夹：

```
com.yoursweakfoe.common.security
├── SecurityAutoConfiguration   # 自动装配（@AutoConfiguration，零信任资源服务器链）
├── SecurityProperties          # 配置属性（ywf.security.*：enabled / roles-claim / authority-prefix）
├── context/                    # 安全上下文
│   └── SecurityUtil            # 按名字读取当前 JWT claim
└── jwt/                        # 验签可插拔
    └── DelegatingJwtDecoder    # 按 JOSE 头 alg 分发验签
```

### 身份流转（南北向 + 东西向）

```
客户端 → 认证服务（签发 JWT）
       → Higress 网关（PEP：转发 JWT，不注入身份 Header）
       → 服务（BearerTokenAuthenticationFilter + JwtDecoder 自验签 → Jwt）
       └ 东西向 → HTTP（一期 RestClient 直连；Feign 经 common-cloud opt-in，RequestInterceptor 自动透传同一 JWT）→ 下游服务自验签
```

### 身份模型：不投影，原生 Jwt

principal 是 Spring Security 原生 `Jwt`——**它本身就是 claims 全量映射表**（`getClaims()` 返回 `Map<String,Object>`），字段名 / 数量随意，不做任何固定结构投影。

`SecurityUtil` 提供按名字读取的泛型方法，字段由各服务自取：

```java
String uid        = SecurityUtil.getString("uid");            // 数值自动归一为字符串
String dept       = SecurityUtil.getString("department");     // 任意字段，缺失返回 null
List<String> role = SecurityUtil.getStringList("roles");      // 数组或逗号串，缺失返回空列表
Jwt jwt           = SecurityUtil.getJwt();                    // 原始 Jwt，全量 claims 逃生舱
```

没有用户名、只有 userId？——`getString("uname")` 返回 null，不炸；多了部门分部？——`getString("department")` 照读。

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

| 方法 | 说明 | 缺失 / 匿名时 |
|------|------|-------------|
| `getJwt()` | 原始已验签 JWT（全量 claims） | `null` |
| `getClaim(name)` | 任意字段原值 | `null` |
| `getString(name)` | 任意字段字符串（数值归一） | `null` |
| `getStringList(name)` | 任意字段列表（数组/逗号串） | 空 List |

### 自动装配

`SecurityAutoConfiguration` 注册：

| Bean | 条件 | 说明 |
|------|------|------|
| `JwtAuthenticationConverter` | `@ConditionalOnMissingBean` | 角色 claim（名与权限前缀可配）→ `ROLE_*` 权限，principal 保持原生 `Jwt`；消费方自定义 Bean 则框架退位 |
| 资源服务器 `SecurityFilterChain` | `@ConditionalOnMissingBean(SecurityFilterChain)` | `oauth2ResourceServer().jwt()` + CSRF 关闭 + 无状态 + permit-all |
| `@EnableWebSecurity` / `@EnableMethodSecurity` | 随配置类 `ywf.security.enabled` 门控（缺省启用） | 启用 `@PreAuthorize` / `@Secured`；`enabled=false` 时整类不激活 |

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

### 配置：角色 claim 名 + 权限前缀（可选，唯一字段缝）

```yaml
ywf:
  security:
    enabled: true            # 安全链总开关，默认 true（缺省启用）
    roles-claim: roles       # 角色列表所在的 claim 名，默认 roles
    authority-prefix: ROLE_  # 角色 → 权限前缀，默认 ROLE_
```

其余身份字段不配置、不写死——各服务按名字自取。

> **opt-out 门控**：`ywf.security.enabled=false` 时整条安全链不注册（无 `SecurityFilterChain`、
> 不启用 `@EnableWebSecurity`/`@EnableMethodSecurity`）。面向「不想要安全链、只想复用 `SecurityUtil`
> 读 JWT」的消费方（如纯内部服务）。`SecurityUtil` 是静态工具类，与安全链无耦合，关闭后仍可用。

### 场景 1：获取当前用户身份（字段自取）

```java
// 契约接口（真实 OrderController）：身份不进方法签名——principal 由安全链承载，Controller 纯透传
@GetMapping("/orders/{orderId}")
OrderCO getOrder(@PathVariable("orderId") UUID orderId);

// Application / Adapter 层：SecurityUtil 按名字自取（domain 层禁止——R6 领域不感知认证上下文）
String userId = SecurityUtil.getString("uid");   // 或 "sub" / "user_id" / 任意你们的名字
Order order = new Order(command, userId);        // 审计字段、数据归属
```

### 场景 2：方法级鉴权

```java
@PreAuthorize("hasRole('ADMIN')")
public void approve(OrderCommand command) { ... }

// 数据归属判断（域数据依赖，无法上浮网关）
boolean isOwner = order.getCustomerId().equals(SecurityUtil.getString("uid"));
boolean isAdmin = SecurityUtil.getStringList("roles").contains("ADMIN");
```

### 场景 3：自定义安全链

```java
@Configuration
@EnableWebSecurity
class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter converter) {
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
- **机制与字段分离**：框架抽象「验签 / 建身份 / 无状态链 / 方法级鉴权」；「密钥方案 / 字段命名 / 字段数量」全部可插拔、按需读取
- **身份即 Jwt，不投影**：principal 是原生 `Jwt`（claims 全量映射表），不投影成固定 record——字段数量不定就投影不了
- **fail-closed**：坏 token → 401，绝不静默放行（对比旧框架的 `catch (Exception ignored)`）
- **边界 permit-all + 方法级鉴权**：路由级在网关，服务用 `@PreAuthorize` 做细粒度鉴权

## 6. 设计决策

### ADR-0001 Header 透传身份（网关验签 + 服务信任 Header）

- 状态：已废弃，由 ADR-0005 取代（零信任：不信任网络、每跳验证）。

### ADR-0003 边界 permit-all SecurityFilterChain

- 状态：accepted

**决策**：路由级鉴权在网关；服务层提供 permit-all + 无状态链，可被覆盖；细粒度鉴权用 `@PreAuthorize`。

### ADR-0005 零信任：服务自验 JWT（资源服务器）

- 状态：accepted

**决策**：服务下沉为 OAuth2 资源服务器，自行验签 JWT；网关降为 PEP。

### ADR-0006 身份不投影：原生 Jwt + 按名字自取

- 状态：accepted

**背景**：公司 JWT 字段命名无规范（`uid`/`uname`）、字段数量不定（可能只有 userId、可能带部门分部、可能无用户名）。若框架投影成固定 record（如 `CurrentUser(userId, username, roles)`），字段一多一少就失配。

**决策**：不投影固定结构。principal 保持原生 `Jwt`（claims 全量映射表），`SecurityUtil` 提供 `getClaim` / `getString` / `getStringList` 按名字读取（缺失返回 null/空）。唯一的字段缝是「角色 → 权限」（`@PreAuthorize` 需要），角色 claim 名经 `ywf.security.roles-claim` 配置。

### ADR-0007 验签可插拔：JwtDecoder 抽象 + 多方案分发

- 状态：accepted

**背景**：不同来源 JWT 使用不同签名算法（HS256 / RS256 …），密钥方案未统一。

**决策**：`JwtDecoder` 接口即抽象；`DelegatingJwtDecoder` 按 JOSE 头 `alg` 分发到各方案 decoder。可选工具类，谁需要谁 `new`。

## 7. 职责边界与技术债

| 项 | 说明 |
|---|---|
| 边界：JWT 签发 / 刷新 / 登出 | 由独立认证服务（IdP）处理，**服务侧不提供签发能力**（旧框架 `generateToken` 下发到服务是反例） |
| 边界：路由级鉴权 | 由网关（PEP）处理 |
| 边界：RBAC 权限模型 | 角色/权限管理属于业务域，各服务按需实现 |
| 边界：数据权限（行级过滤） | 与业务模型强耦合，由业务层 SQL 条件自行实现 |
| 边界：机器身份（client-credentials） | 定时任务 / MQ 等无用户上下文的调用，需走 client-credentials 取 token，另行设计 |
| 演进：token 交换 / 受众限制 | 东西向透传同一 JWT 为基线；按需引入 RFC 8693 token exchange 或 audience/scope 限制 |
| 风险：HMAC 共享密钥 | 对称密钥意味着「验签方 = 签收方」，任一服务被攻破即可伪造 token；长远可迁非对称（JWKS） |
