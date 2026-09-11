# common-exception

统一异常体系：业务异常定义（`BusinessException`）+ REST 全局异常处理，错误响应按 RFC 9457，自动装配。

> 本文分两段：§1–4 面向使用者（怎么用），§5–7 面向设计者（为什么这么设计）。

## 1. 定位与边界

本包给所有微服务提供统一的异常定义和错误响应翻译。需要抛出业务异常或对外暴露 REST 接口的服务都应引入，引入即自动生效。

> 限流/熔断异常（429/503）不在本包：入口限流由 Higress 网关承担，不属于基础异常体系。

## 2. 核心能力

### BusinessException

| 方法/字段 | 说明 |
|---------|------|
| `BusinessException(String messageKey)` | 构造器，无参数，状态缺省 422 |
| `BusinessException(String messageKey, Map<String,Object> params)` | 构造器，携带占位符参数，状态缺省 422 |
| `BusinessException(String messageKey, int httpStatus)` | 构造器，显式指定 HTTP 状态，如 404 / 409 |
| `BusinessException(String messageKey, Map<String,Object> params, int httpStatus)` | 构造器，占位符参数 + 显式 HTTP 状态 |
| `getMessage()` | 返回 messageKey，位点格式 `"{aggregate}:err.{scene}"`，如 `"payment:err.notFound"` |
| `getParams()` | 返回占位符参数，不可变 Map，空表示无插值 |
| `getHttpStatus()` | 返回显式指定的 HTTP 状态；未指定时为 `null`，REST 通道缺省映射 422 |

### GlobalRestExceptionHandler（REST 通道）

`@RestControllerAdvice`，将异常翻译为 RFC 9457 HTTP 响应。本表是 docs 侧异常 → HTTP 映射的唯一完整副本，与 `GlobalRestExceptionHandler` 类 javadoc 映射表、`@ExceptionHandler` 实配一一对齐。其余文档需要时指针引用本表，不复制行：

| 异常类型 | HTTP 状态码 | title |
|---------|:-----------:|-------|
| `BusinessException` | 异常自带状态，缺省 422 | Business Error |
| `ConstraintViolationException` | 400 | Validation Failed |
| `MethodArgumentNotValidException` | 400 | Validation Failed |
| `BindException` | 400 | Validation Failed |
| `HttpMessageNotReadableException` | 400 | Bad Request |
| `MissingServletRequestParameterException` | 400 | Bad Request |
| `MethodArgumentTypeMismatchException` | 400 | Bad Request |
| `NoResourceFoundException` | 404 | Not Found |
| `HttpRequestMethodNotSupportedException` | 405 | Method Not Allowed |
| `HttpMediaTypeNotSupportedException` | 415 | Unsupported Media Type |
| `IllegalStateException` | 409 | Conflict |
| `SilentWriteLossException` | **500（独立通道）** | Internal Server Error |
| `IllegalArgumentException` | 400 | Bad Request |
| 其他未捕获异常（`Exception` 兜底） | 500 | Internal Server Error |

> `MethodArgumentNotValidException` 是 `BindException` 的子类。`@RequestBody` 校验失败优先命中专属 handler，纯参数绑定失败由 `BindException` 承接。两者都返回 400 + `fieldErrors` 扩展成员，detail 为稳定泛化文案。

> `MethodArgumentTypeMismatchException`：路径变量或请求参数的类型转换失败，如非法 UUID、错误枚举名。不处理会落入兜底 500；客户端传参错误必须返回 400。detail 仅回显参数名与期望类型，即 `Parameter '{参数名}' must be of type {期望类型}`，不回显客户端原始值。

> `SilentWriteLossException`（静默写丢失）单列 500 通道，刻意不入 ISE 的 409 通道。INSERT / DELETE 影响 0 行是合法语句的不可能状态：写丢失、schema 事故或调用链缺陷，**重试无意义、需人工介入**。它按 ERROR 日志（带栈）记录，作为运维告警抓取信号；若混入 409/WARN 通道将静默躲过告警。对外仅回泛化 500 文案，原始消息含实体 ID、SQL 语义字样，只进服务端日志。抛出链路与三分通道见 `common-ddd.md` §2 仓储支撑。

响应格式，Content-Type 为 `application/problem+json`：

> §2–§3 示例用虚构教例 Payment 家族，与 `knowledge/docs/how-to/new-aggregate.md` 同族。虚构教例，sample 未实现；真实例形态见 sample-application，结构同构。`payment:err.*` 是它的错误码位点。

```json
{
  "type": "about:blank",
  "title": "Business Error",
  "status": 422,
  "detail": "payment:err.statusPending",
  "instance": "/api/payments",
  "params": { "current": "FAILED", "required": "PENDING" }
}
```

## 3. 使用方式

> **严格规范在法卷**：本节正文已升入法卷 → [../../../specs/current/modules/exception.md](../../../specs/current/modules/exception.md)。条款、代码形状、禁令以法卷为准，本字典条目只留面向人的宽松指引。

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

- **i18n 位点而非硬编码文案**：`messageKey` 是前端翻译 key。服务端不维护 messages.properties，由前端 `t(key, params)` 渲染
- **RFC 9457 响应格式**：标准化 HTTP 错误响应（`application/problem+json`），外部消费方可程序化处理
- **自动装配**：引入依赖即生效，无需 `@Import` 或手动配置

## 6. 设计决策（已迁出）

> 本模块的历史决策日志已随案卷清理归零，旧编号制已废。当时的裁决快照在封存案卷 §裁决记录，今天仍成立的论证在 `docs/explanation/`。地图只留指针，本区不维护决策正文。

## 7. 职责边界与技术债

| 项 | 说明 |
|---|---|
| 边界：限流/熔断异常（429/503） | 由 Higress 网关处理，不纳入基础异常体系 |
