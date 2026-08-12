# common-security

网关协同安全 —— Higress JWT 验签透传模式下的身份解析（REST 边界）与 gRPC 东西向身份传播。

## 定位

在 Higress 网关验签透传模式下，为微服务提供用户身份解析和服务间身份传播能力。
面向所有需要获取当前用户身份的业务服务。
REST 入站运行在 Servlet 过滤器链（Spring MVC 边界），gRPC 入站/出站运行在 gRPC 全局拦截器链。

## 设计原则

- **网关验签，服务信任**：微服务不持有密钥，不做验签；只解析网关透传的 Header
- **身份来源标记**：SecurityContext 中的身份携带来源（`EDGE` = REST 边界一手身份 / `PROPAGATED` = gRPC 上游传递身份）
- **自动传播**：东西向调用通过 gRPC 全局拦截器对自动传播身份，下游服务零配置；多跳链路由每一跳的出站拦截器逐跳重新写入 Metadata
- **边界 permit-all**：鉴权决策收口在网关，服务层提供 permit-all + 无状态的 SecurityFilterChain（可被服务自定义链覆盖）

## 包结构

```
com.yoursweakfoe.common.security/
├── AuthConstants.java                          ← Header / Metadata Key / 角色前缀常量（单一定义处）
├── IdentitySource.java                         ← 身份来源枚举（EDGE / PROPAGATED）
├── IdentityDetails.java                        ← Authentication details 载荷（username + source）
├── SecurityUtil.java                           ← 从 SecurityContext 获取当前用户身份与来源
├── SecurityContextSupport.java                 ← 两个入站通道共用的角色解析 + Context 建立/清理
├── SecurityAutoConfiguration.java              ← Spring Boot 自动装配（AutoConfiguration.imports 注册）
├── web/
│   └── SecurityWebFilter.java                  ← **REST 入站**：OncePerRequestFilter，Header → SecurityContext（source=EDGE）
└── grpc/
    ├── SecurityMetadata.java                   ← sec_* Metadata.Key 定义
    ├── GrpcSecurityServerInterceptor.java      ← **gRPC 入站**：Metadata → SecurityContext（source=PROPAGATED）
    └── GrpcSecurityClientInterceptor.java      ← gRPC 出站：SecurityContext → Metadata
```

### 两条入站通道的职责分界

| 通道 | 负责组件 | 身份来源 | 来源标记 |
|------|---------|---------|---------|
| REST 入站（网关边界） | `SecurityWebFilter` | 网关透传 Header | `EDGE` |
| gRPC 入站（inter-service） | `GrpcSecurityServerInterceptor` | gRPC Metadata | `PROPAGATED` |

两通道相互独立、互不依赖；身份可信源只在网关进入系统一次，gRPC 链路传递的是已验证身份。

**gRPC 线程模型约束**：gRPC 业务回调（`onMessage` / `onHalfClose` 等）由传输层线程池调度，
不与 `interceptCall` 同线程。`GrpcSecurityServerInterceptor` 因此以监听器代理在**每个回调前后**
建立/清理 SecurityContext（而非一次性建立），保证业务代码落在任意回调线程都能经 `SecurityUtil` 读到身份。

## 核心功能

### 南北向（网关 → 服务）

```
客户端 → Higress 网关（jwt-auth 插件验签 + claims_to_headers 透传）
    → Spring MVC（SecurityWebFilter 解析 Header → SecurityContext，source=EDGE）
```

### 东西向（服务 → 服务，gRPC）

```
服务A（SecurityContext 有身份）
    → GrpcSecurityClientInterceptor（SecurityContext → Metadata）
    → 服务B（GrpcSecurityServerInterceptor：Metadata → SecurityContext，source=PROPAGATED）
        → GrpcSecurityClientInterceptor（SecurityContext → Metadata）
        → 服务C（GrpcSecurityServerInterceptor：Metadata → SecurityContext）
```

多跳链路（A→B→C）无需专门透传组件：B 的业务代码运行在已建立上下文的回调内，
其发起的下游调用由出站拦截器从本地 SecurityContext 重新写入 Metadata，逐跳自动完成。

### 透传 Header（REST 入站）

常量定义于 `AuthConstants`：

| 常量 | Header | JWT Claim | 说明 |
|------|--------|-----------|------|
| `HDR_USER_ID` | `X-User-Id` | `sub` | 用户 ID |
| `HDR_USERNAME` | `X-Username` | `username` | 用户名 |
| `HDR_ROLES` | `X-Roles` | `roles` | 角色列表（逗号分隔） |

### 东西向传播 Metadata

常量同样定义于 `AuthConstants`（与 Header 并列，避免两侧改名遗漏导致传播链静默断裂）：

| 常量 | Metadata Key | 来源 | 说明 |
|------|--------------|------|------|
| `METADATA_USER_ID` | `sec_user_id` | principal | 用户 ID |
| `METADATA_USERNAME` | `sec_username` | details | 用户名 |
| `METADATA_ROLES` | `sec_roles` | authorities | 角色列表（逗号分隔，不含 `ROLE_` 前缀） |
| `METADATA_PREFIX` | `sec_` | — | 身份载荷识别前缀 |
| `ROLE_PREFIX` | `ROLE_` | — | Spring Security 角色前缀；传播载荷不含前缀，写入 `GrantedAuthority` 时补上、读出时剥离 |

### 自动装配

`SecurityAutoConfiguration` 经 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册：

| Bean | 条件 | 说明 |
|------|------|------|
| `SecurityWebFilter` | Servlet Web 应用 | Boot 自动注册进 Servlet 过滤器链 |
| permit-all `SecurityFilterChain` | Servlet Web 应用 + 无自定义链 | CSRF 关闭 + 无状态 + 全放行；`beforeName` 先于 Boot 默认安全链注册使其退避 |
| `GrpcSecurityServerInterceptor` | classpath 存在 grpc-api | `@GlobalServerInterceptor` + `@Order(-100)` |
| `GrpcSecurityClientInterceptor` | classpath 存在 grpc-api | `@GlobalClientInterceptor` + `@Order(-100)` |

## 使用方式

```xml
<dependency>
    <groupId>com.yoursweakfoe</groupId>
    <artifactId>common-security</artifactId>
</dependency>
```

引入即生效。Filter 与 interceptor 由自动装配注册，业务代码通过 `SecurityUtil` 获取身份。

### 安全前提条件（必须满足）

1. 网关必须配置 jwt-auth 插件（Higress / Kong / APISIX），在转发前完成 JWT 验签并注入 X-User-* Header
2. 服务端口（REST 与 gRPC）不得直接暴露到公网（必须经网关转发，否则 Header / Metadata 可被伪造）
3. 若网关配置缺失或被绕过，本模块不会拒绝请求（无 Header 时建立匿名上下文）——这是设计取舍：安全责任统一收口在网关

### SecurityUtil 未登录行为

| 方法 | 未登录时返回 | 说明 |
|------|------------|------|
| `getCurrentUserId()` | `null` | SecurityContext 为空或 principal 为 null |
| `getUsername()` | `null` | details 未设置 |
| `getIdentitySource()` | `null` | 非本框架建立的身份亦返回 null |
| `getRoles()` | 空 List | authorities 为空时返回 `List.of()` |

> 业务代码应自行判断 null/空（如 `if (userId == null) throw new BusinessException("auth:err.notLoggedIn")`）。

### 场景 1：获取当前用户身份

```java
@Service
public class OrderAppService {

    public OrderCO placeOrder(PlaceOrderCommand command) {
        // 在任意业务代码中获取当前登录用户
        String userId = SecurityUtil.getCurrentUserId();
        String username = SecurityUtil.getUsername();
        // 用于审计字段填充、数据归属等
        Order order = new Order(command, userId);
        // ...
    }
}
```

### 场景 2：角色判断

```java
@Component
public class CancelOrderHandler implements CommandHandler<CancelOrderCommand, OrderDTO> {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDTO handle(CancelOrderCommand command) {
        Order order = orderRepository.findById(command.getOrderId())
                .orElseThrow(() -> new BusinessException("order:err.notFound"));

        // 非本人且非管理员不允许取消
        String currentUserId = SecurityUtil.getCurrentUserId();
        List<String> roles = SecurityUtil.getRoles();
        boolean isOwner = order.getCustomerId().equals(currentUserId);
        boolean isAdmin = roles.contains("ADMIN");
        if (!isOwner && !isAdmin) {
            throw new BusinessException("order:err.forbidden");
        }

        order.cancel(command.getReason());
        orderRepository.update(order);
        return orderAssembler.toDTO();
    }
}
```

### 场景 3：东西向调用（自动传播，零配置）

```java
// 服务 A 调用服务 B（gRPC stub），身份自动透传，无需手动传递
private final PaymentInternalServiceGrpc.PaymentInternalServiceBlockingStub paymentStub;

public void pay(UUID orderId) {
    // SecurityContext 中的身份由 GrpcSecurityClientInterceptor 自动写入 Metadata
    // 服务 B 的 GrpcSecurityServerInterceptor 自动重建 SecurityContext（source=PROPAGATED）
    paymentStub.createPayment(CreatePaymentRequest.newBuilder()
            .setOrderId(orderId.toString())
            .build());
}
```

> 多跳链路（A→B→C）逐跳自动透传（每跳出站拦截器从本地 SecurityContext 重新写入 Metadata），业务代码无感知。

## 设计决策与未实现功能

| 决策 | 理由 |
|------|------|
| 网关验签 + 服务信任 Header | 微服务不持有密钥，职责单一；密钥管理集中在网关 |
| 身份来源标记（EDGE / PROPAGATED） | 区分一手身份与传递身份，安全审计与边界策略可据此决策 |
| 边界 permit-all SecurityFilterChain | 鉴权在网关；服务层重复验签无收益。服务可声明自己的 SecurityFilterChain 覆盖 |
| 监听器代理建立/清理上下文 | gRPC 回调线程不固定，逐回调包裹保证任意线程可见且线程池不泄漏 |
| **未实现** JWT 验签 / Token 刷新 | 验签由 Higress 网关 jwt-auth 插件统一处理 |
| **未实现** RBAC 权限模型 | 角色/权限管理属于业务域，各服务按需实现；本模块只提供 Header/Metadata→Context 桥接 |
| **未实现** OAuth2 / SSO 登录流程 | 登录由独立认证服务 + 网关处理，业务微服务不参与 |
| **未实现** URL 级权限控制 | URL 鉴权由网关处理；服务内方法级鉴权可用 `@PreAuthorize` |
| **未实现** 数据权限（行级过滤） | 数据权限与业务模型强耦合，由业务层 SQL 条件自行实现 |

## 依赖关系

```
common-security → spring-boot-starter-security (compile，含 spring-security-web)
                → spring-boot-autoconfigure (compile)
                → spring-web (optional：OncePerRequestFilter)
                → jakarta.servlet-api (provided)
                → grpc-api (optional：纯 REST 服务无需)
                → spring-grpc-core (optional：@GlobalServerInterceptor / @GlobalClientInterceptor 注解)
```

> `spring-security-web` 必须保留：Boot 4 的 `ServletWebSecurityAutoConfiguration` 在 servlet web
> 应用下会内省 `WebSecurityConfiguration`，缺失 `SecurityFilterChain` 类将导致启动失败。
