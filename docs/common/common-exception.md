# common-exception

统一异常体系 —— 业务异常定义（BusinessException）+ REST 全局异常处理（RFC 9457，自动装配）。

> 本文分两段：§1–4 面向使用者（怎么用），§5–7 面向设计者（为什么这么设计）。

## 1. 定位与边界

为所有微服务提供统一的异常定义和错误响应翻译。任何需要抛出业务异常或对外暴露 REST 接口的服务都应引入，引入即自动生效。

> 限流/熔断异常（429/503）不在本包：入口限流由 Higress 网关承担，不属于基础异常体系。

## 2. 核心能力

### BusinessException

| 方法/字段 | 说明 |
|---------|------|
| `BusinessException(String messageKey)` | 构造（无参数，状态缺省 422） |
| `BusinessException(String messageKey, Map<String,Object> params)` | 构造（携带占位符参数，状态缺省 422） |
| `BusinessException(String messageKey, int httpStatus)` | 构造（显式 HTTP 状态，如 404 / 409） |
| `BusinessException(String messageKey, Map<String,Object> params, int httpStatus)` | 构造（占位符参数 + 显式 HTTP 状态） |
| `getMessage()` | 返回 messageKey（如 `"order:err.insufficientStock"`） |
| `getParams()` | 返回占位符参数（不可变 Map，空表示无插值） |
| `getHttpStatus()` | 返回显式指定的 HTTP 状态；未指定时为 `null`（REST 通道缺省映射 422） |

### GlobalRestExceptionHandler（REST 通道）

`@RestControllerAdvice`，将异常翻译为 RFC 9457 HTTP 响应：

| 异常类型 | HTTP 状态码 | title |
|---------|:-----------:|-------|
| `BusinessException` | 异常自带状态，缺省 422 | Business Error |
| `ConstraintViolationException` | 400 | Validation Failed |
| `MethodArgumentNotValidException` | 400 | Validation Failed |
| `IllegalStateException` | 409 | Conflict |
| `IllegalArgumentException` | 400 | Bad Request |
| `MethodArgumentTypeMismatchException` | 400 | Bad Request |
| 其他未捕获异常 | 500 | Internal Server Error |

> `MethodArgumentTypeMismatchException`：路径变量 / 请求参数类型转换失败（如非法 UUID、错误枚举名）。不处理时会落入兜底 500——客户端传参错误必须返回 400，detail 仅回显参数名与期望类型（`Parameter '{参数名}' must be of type {期望类型}`），不回显客户端原始值。

响应格式（Content-Type: `application/problem+json`）：

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

## 3. 使用方式

```xml
<dependency>
    <groupId>com.yoursweakfoe</groupId>
    <artifactId>common-exception</artifactId>
</dependency>
```

引入即生效。REST 异常处理器由 `ExceptionAutoConfiguration` 自动注册。

### 场景 1：抛出业务异常

```java
throw new BusinessException("order:err.notFound");

throw new BusinessException("order:err.insufficientStock",
        Map.of("sku", "A001", "required", 10, "available", 3));

// 显式指定 HTTP 状态（默认 422）
throw new BusinessException("order:err.notFound", 404);
throw new BusinessException("order:err.alreadyConfirmed",
        Map.of("id", "A001"), 409);
```

> **安全注意**：`params` 内容会序列化到 HTTP 响应体，禁止放入敏感信息。

### 场景 2：领域层显式抛出

```java
public class Order extends AggregateRoot<UUID> {
    public void pay() {
        requireStatus("order:err.status.pending", OrderStatus.PENDING);
        this.status = OrderStatus.PAID;
    }
}
```

## 4. 依赖关系

```
common-exception → spring-boot-autoconfigure（AutoConfiguration）
                 → spring-boot-starter-validation（ConstraintViolationException + Hibernate Validator，compile 传递）
                 → spring-web（optional：@RestControllerAdvice / ResponseEntity）
                 → jakarta.servlet-api（provided：请求 URI 读取）
                 → tools.jackson.core:jackson-databind（test：MockMvc 消息转换）
                 → spring-webmvc（test：MockMvc standalone 验证）
```

## 5. 设计原则

- **i18n 位点而非硬编码文案**：`messageKey` 为前端翻译 key，服务端不维护 messages.properties，由前端 `t(key, params)` 渲染
- **RFC 9457 响应格式**：标准化 HTTP 错误响应（`application/problem+json`），外部消费方可程序化处理
- **自动装配**：引入依赖即生效，无需 `@Import` 或手动配置

## 6. 设计决策

### ADR-0001 i18n 位点（字符串 key）而非数字错误码

- 状态：accepted

**背景**：错误码用字符串 key 还是数字。

**选项**：
- 数字错误码：紧凑，但多语言扩展需映射表
- 字符串 key：天然支持多语言，前端直接翻译

**决策**：选字符串 key。服务端不维护 messages.properties，前端负责渲染。

**确认**：`BusinessException` 持有 `messageKey` 字符串。

### ADR-0002 RFC 9457 响应格式

- 状态：accepted

**背景**：REST 错误响应采用何种格式。

**决策**：采用 RFC 9457（`application/problem+json`），`type` 当前为 `about:blank`，待错误类型文档化后替换为绝对 URI；`params`/`fieldErrors` 为合规扩展字段。

**确认**：`GlobalRestExceptionHandler` 响应载体为 Spring 内建 `ProblemDetail`（`ResponseEntity<ProblemDetail>`，RFC 9457 标准成员 `type`/`title`/`status`/`detail`/`instance`），`params`/`fieldErrors` 经 `ProblemDetail` 扩展属性位（`setProperty`）注入为合规扩展成员（RFC 9457 §3.2），Content-Type 显式声明 `application/problem+json`。技术类异常的 detail 为稳定泛化文案（原始消息只进服务端日志，防内部信息外泄）。

### ADR-0003 IllegalStateException → 409

- 状态：accepted

**背景**：乐观锁版本冲突（UPDATE 影响行数 0 且实体仍在）如何映射 HTTP 状态。

**决策**：`IllegalStateException` → 409 Conflict——409 通道即为乐观锁冲突预留（`OptimisticLockConflictException` 继承自它，「实体消失」的普通 ISE 同走此通道）；状态机非法转换属业务规则违反，聚合根抛 `BusinessException` 走缺省 422，不占用 409。

**确认**：`GlobalRestExceptionHandler` 处理 `IllegalStateException` 返回 409；sample 聚合状态守卫（如 `Order.pay()`）抛 `BusinessException`（`order:err.*`）→ 422。

## 7. 职责边界与技术债

| 项 | 说明 |
|---|---|
| 边界：限流/熔断异常（429/503） | 由 Higress 网关处理，不纳入基础异常体系 |
