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

> **严格规范在法卷**：本节正文已入法 → [../../../specs/current/modules/
security
.md](../../../specs/current/modules/
security
.md)（条款、代码形状、禁则以法卷为准）。本字典架只余宽松语感。

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

## 6. 设计决策（已迁出）

> 本模块全部决策日志已迁至 [`knowledge/decisions/`](../../../decisions/README.md)（全局编号 ADR-NNNN；旧号映射见该文 §migration）。归属法：判例住卷宗，地图只留指针——本区不再维护决策正文。

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
