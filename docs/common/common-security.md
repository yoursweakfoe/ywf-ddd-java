# common-security

网关协同安全 —— Higress JWT 验签透传模式下的身份解析（REST 边界）。

> 本文分两段：§1–4 面向使用者（怎么用），§5–7 面向设计者（为什么这么设计）。

## 1. 定位与边界

在 Higress 网关验签透传模式下，为微服务提供用户身份解析能力。面向所有需要获取当前用户身份的业务服务。REST 入站运行在 Servlet 过滤器链（Spring MVC 边界）。

> 本模块不做验签、不做鉴权、不做 RBAC：安全责任统一收口在网关，服务层只解析网关透传的 Header 并建立身份上下文。

## 2. 核心能力

### 身份流转（南北向：网关 → 服务）

```
客户端 → Higress 网关（jwt-auth 插件验签 + claims_to_headers 透传）
    → Spring MVC（SecurityWebFilter 解析 Header → SecurityContext，source=EDGE）
```

### 透传 Header（REST 入站）

常量定义于 `AuthConstants`：

| 常量 | Header | JWT Claim | 说明 |
|------|--------|-----------|------|
| `HDR_USER_ID` | `X-User-Id` | `sub` | 用户 ID |
| `HDR_USERNAME` | `X-Username` | `username` | 用户名 |
| `HDR_ROLES` | `X-Roles` | `roles` | 角色列表（逗号分隔） |

另有 `ROLE_PREFIX`（`ROLE_`）：Spring Security 角色前缀，写入 `GrantedAuthority` 时补上、读出时剥离。

### SecurityUtil 未登录行为

| 方法 | 未登录时返回 |
|------|------------|
| `getCurrentUserId()` | `null` |
| `getUsername()` | `null` |
| `getIdentitySource()` | `null` |
| `getRoles()` | 空 List |

### 自动装配

`SecurityAutoConfiguration` 注册：

| Bean | 条件 | 说明 |
|------|------|------|
| `SecurityWebFilter` | Servlet Web 应用 | Boot 自动注册进 Servlet 过滤器链 |
| permit-all `SecurityFilterChain` | Servlet Web 应用 + 无自定义链 | CSRF 关闭 + 无状态 + 全放行；`beforeName` 先于 Boot 默认安全链注册使其退避 |

## 3. 使用方式

```xml
<dependency>
    <groupId>com.yoursweakfoe</groupId>
    <artifactId>common-security</artifactId>
</dependency>
```

### 安全前提条件（必须满足）

1. 网关必须配置 jwt-auth 插件，在转发前完成 JWT 验签并注入 `X-User-*` Header
2. 服务端口不得直接暴露到公网（必须经网关转发，否则 Header 可被伪造）
3. 若网关配置缺失或被绕过，本模块不会拒绝请求（无 Header 时建立匿名上下文）

### 场景 1：获取当前用户身份

```java
String userId = SecurityUtil.getCurrentUserId();
String username = SecurityUtil.getUsername();
Order order = new Order(command, userId);  // 审计字段、数据归属
```

### 场景 2：角色判断

```java
String currentUserId = SecurityUtil.getCurrentUserId();
List<String> roles = SecurityUtil.getRoles();
boolean isOwner = order.getCustomerId().equals(currentUserId);
boolean isAdmin = roles.contains("ADMIN");
if (!isOwner && !isAdmin) {
    throw new BusinessException("order:err.forbidden");
}
```

## 4. 依赖关系

```
common-security → spring-boot-starter-security（含 spring-security-web）
                → spring-boot-autoconfigure
                → spring-web（optional：OncePerRequestFilter）
                → jakarta.servlet-api（provided）
```

## 5. 设计原则

- **网关验签，服务信任**：微服务不持有密钥，不做验签；只解析网关透传的 Header
- **身份来源标记**：SecurityContext 中的身份携带来源标记（`EDGE` = REST 边界一手身份）
- **边界 permit-all**：鉴权决策收口在网关，服务层提供 permit-all + 无状态 SecurityFilterChain（可被服务自定义链覆盖）

## 6. 设计决策

### ADR-0001 网关验签 + 服务信任 Header

- 状态：accepted

**背景**：JWT 验签放在网关还是服务。

**决策**：验签由网关完成，服务只信任网关透传的 Header。微服务不持有密钥，职责单一；密钥管理集中在网关。

**后果**：服务端口必须不直暴公网，否则 Header 可伪造（见 §3 安全前提）。

**确认**：`SecurityWebFilter` 只解析 Header，不含任何验签逻辑。

### ADR-0002 身份来源标记（EDGE）

- 状态：accepted

**背景**：是否需要区分身份的来源（网关边界 vs 东西向传播）。

**决策**：引入 `IdentitySource` 标记，当前仅 `EDGE`（REST 边界一手身份）。安全审计与边界策略可据此决策。

**确认**：`IdentityDetails` 携带 `source` 字段，`SecurityUtil.getIdentitySource()` 返回来源。

### ADR-0003 边界 permit-all SecurityFilterChain

- 状态：accepted

**背景**：服务层是否重复鉴权。

**决策**：不重复鉴权。鉴权在网关，服务层提供 permit-all + 无状态链（CSRF 关闭），可被服务自定义链覆盖。

**确认**：`SecurityAutoConfiguration` 提供 `@ConditionalOnMissingBean` 的 permit-all 链。

## 7. 职责边界与技术债

| 项 | 说明 |
|---|---|
| 边界：东西向 HTTP 身份传播 | 未来设计；当前身份仅在网关边界解析，架构稳定后再扩展 |
| 边界：JWT 验签 / Token 刷新 | 由 Higress 网关 jwt-auth 插件统一处理 |
| 边界：RBAC 权限模型 | 角色/权限管理属于业务域，各服务按需实现 |
| 边界：OAuth2 / SSO 登录流程 | 由独立认证服务 + 网关处理 |
| 边界：URL 级权限控制 | 由网关处理；服务内方法级鉴权可用 `@PreAuthorize` |
| 边界：数据权限（行级过滤） | 与业务模型强耦合，由业务层 SQL 条件自行实现 |
