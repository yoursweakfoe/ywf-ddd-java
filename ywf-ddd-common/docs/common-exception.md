# common-exception

## 模块定位

统一异常体系 —— 业务异常定义 + REST/gRPC 双通道全局异常处理（Spring Boot AutoConfiguration 自动注册）。

为所有微服务提供统一的异常定义和错误响应翻译。任何需要抛出业务异常或对外暴露 REST/gRPC 接口的服务都应引入。引入即自动生效，无需任何配置。

设计原则：
- **i18n 位点而非硬编码文案**：`messageKey` 为前端翻译 key，服务端不维护 `messages.properties`，由前端 `t(key, params)` 渲染
- **RFC 9457 响应格式**：标准化 HTTP 错误响应（`application/problem+json`）
- **gRPC 异常不透传内部堆栈**：Consumer 端经客户端拦截器还原 BusinessException（messageKey + params 完整保留）
- **自动装配**：引入依赖即生效，无需 `@Import` 或手动配置

## 核心类表

### BusinessException

| 方法/字段 | 说明 |
|---------|------|
| `BusinessException(String messageKey)` | 构造（无参数） |
| `BusinessException(String messageKey, Map<String,Object> params)` | 构造（携带占位符参数） |
| `getMessage()` | 返回 messageKey（如 `"order:err.insufficientStock"`） |
| `getParams()` | 返回占位符参数（不可变 Map，空表示无插值） |

### GlobalRestExceptionHandler（REST 通道）

`@RestControllerAdvice`，在 Spring MVC 管线中将异常翻译为 **RFC 9457** HTTP 响应。

| 异常类型 | HTTP 状态码 | title |
|---------|:-----------:|-------|
| `BusinessException` | 422 | Business Error |
| `ConstraintViolationException` | 400 | Validation Failed |
| `MethodArgumentNotValidException` | 400 | Validation Failed |
| `IllegalStateException` | 409 | Conflict |
| `IllegalArgumentException` | 400 | Bad Request |
| 其他未捕获异常 | 500 | Internal Server Error |

响应格式（RFC 9457，Content-Type: `application/problem+json`）：

```json
{
  "type": "about:blank",
  "title": "Business Error",
  "status": 422,
  "detail": "order:err.insufficientStock",
  "instance": "/api/orders",
  "params": { "sku": "A001", "required": 10, "available": 3 }
}
```

字段语义：
- `type` — 错误类别 URI（当前为 `about:blank`，待错误类型文档化后可替换为绝对 URI）
- `instance` — 本次具体发生的请求路径
- `params` / `fieldErrors` — 合规扩展字段（RFC 9457 §3.2 允许自定义成员）

### GrpcExceptionServerInterceptor（gRPC 通道，服务端）

全局服务端拦截器（`@GlobalServerInterceptor`，`@Order(-50)`），以监听器代理捕获业务回调异常，映射为 Status + Trailers：

| 异常类型 | Status Code | description | Trailers |
|---------|------|------|------|
| `BusinessException` | FAILED_PRECONDITION | messageKey | `biz-message-key` + `biz-params-bin`（params JSON） |
| `IllegalStateException` | FAILED_PRECONDITION | 原始 message | — |
| `IllegalArgumentException` | INVALID_ARGUMENT | 原始 message | — |
| 其他 RuntimeException | INTERNAL | `"Internal error"`（原始信息仅写日志） | — |

### GrpcExceptionClientInterceptor（gRPC 通道，客户端）

全局客户端拦截器，`onClose` 时检测 Trailers 中的 `biz-message-key`，还原 BusinessException 并挂载到 Status 的 cause。

Consumer 端处理示例：

```java
try {
    ProductInternalInfo info = productStub.getProduct(request);
} catch (StatusRuntimeException e) {
    BusinessException biz = GrpcExceptions.extractBusiness(e);
    if (biz != null) {
        // biz.getMessage() → i18n 位点
        // biz.getParams()  → 占位符参数
    } else {
        throw e; // 系统级错误原样上抛
    }
}
```

> 与 RPC 直传异常对象的差异：gRPC 协议层只传递 Status + Trailers，Consumer 捕获类型为
> `StatusRuntimeException`，BusinessException 位于其 cause（经 `GrpcExceptions.extractBusiness` 提取）。

## 使用方式

```xml
<dependency>
    <groupId>com.yoursweakfoe</groupId>
    <artifactId>common-exception</artifactId>
</dependency>
```

引入即生效。REST 异常处理器 + gRPC 异常拦截器由 `ExceptionAutoConfiguration` 自动注册。

### 场景 1：抛出业务异常

```java
// 基本用法
throw new BusinessException("order:err.notFound");

// 携带参数
throw new BusinessException("order:err.insufficientStock",
        Map.of("sku", "A001", "required", 10, "available", 3));
```

> **安全注意**：`params` 内容会序列化到 HTTP 响应体 / gRPC Trailers。禁止放入敏感信息。

### 场景 2：领域层显式抛出

```java
public class Order extends AggregateRoot<UUID> {
    public void pay() {
        requireStatus("order:err.status.pending", OrderStatus.PENDING);
        this.status = OrderStatus.PAID;
    }
}
```

## 配置项

无运行时配置。`ExceptionAutoConfiguration` 经 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册：

| Bean | 条件 |
|------|------|
| `GlobalRestExceptionHandler` | Servlet Web 应用且存在 DispatcherServlet |
| `GrpcExceptionServerInterceptor` / `GrpcExceptionClientInterceptor` | classpath 存在 grpc-api |

## 设计决策

| 决策 | 理由 |
|------|------|
| i18n 位点（字符串 key）而非数字错误码 | 字符串 key 天然支持多语言扩展；服务端不维护 messages.properties，前端负责渲染 |
| RFC 9457 响应格式 + `application/problem+json` | 标准化错误响应，外部消费方可程序化处理 |
| gRPC 不透传内部异常 | 避免泄漏类名/行号/堆栈；Consumer 仅收到可还原的 BusinessException |
| BusinessException → FAILED_PRECONDITION | 业务前置条件不满足的语义最贴近；messageKey 冗余在 description 与 Trailer 两处 |
| params 经 JSON 写入二进制 Trailer（`biz-params-bin`） | 任意 Map 结构可序列化；-bin 后缀自动 base64，非 ASCII 安全 |
| `IllegalStateException` → 409 / FAILED_PRECONDITION | 乐观锁冲突、状态机非法转换统一映射 |
| 未实现 限流/熔断异常（429/503） | 限流由网关（Higress）处理，不属于基础异常体系 |

## 依赖关系

```
common-exception → spring-boot-autoconfigure（AutoConfiguration）
                 → spring-boot-starter-validation（ConstraintViolationException）
                 → spring-web（optional：@RestControllerAdvice / ResponseEntity）
                 → jakarta.servlet-api（provided：请求 URI 读取）
                 → tools.jackson.databind:jackson-databind（params JSON 编解码）
                 → grpc-api（optional：纯 REST 服务无需）
                 → spring-grpc-core（optional：全局 interceptor 注册注解）
```
