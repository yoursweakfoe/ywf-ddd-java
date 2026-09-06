# 异常全链路

> 设计原理 → [module-design/domain.md](../module-design/domain.md)（异常策略章节）

## 业务场景

> 本文为**虚构教例**（`payment` / `inventory` 聚合，sample 未实现）；示例应用真实错误码前缀见文末真实例注记。

本文展示一个业务异常从领域层产生到前端收到 HTTP 响应的**完整链路**。

**业务规则：**

1. 对处于非 PENDING 状态的支付单发起扣款 → 状态机校验失败
2. 领域层显式抛出 `BusinessException`（携带 i18n 位点）
3. 异常沿调用栈向上传播，由 `GlobalRestExceptionHandler`（`@RestControllerAdvice`）自动翻译为 HTTP 响应
4. 前端收到 RFC 9457 格式的 JSON 错误体，用 `t(messageKey, params)` 渲染本地化文案

## 异常传播链路

```
Payment.charge()
  → if (status != PENDING) throw new BusinessException("payment:err.status.pending")

ChargePaymentHandler.handle(command)
  → payment.charge()  // 异常向上传播（Handler 不 catch）

PaymentAppService.chargePayment(command)
  → chargePaymentHandler.handle(command)  // 继续传播

PaymentController.chargePayment(paymentId)
  → paymentAppService.chargePayment(command)  // 继续传播

Spring MVC 异常解析管线
  → GlobalRestExceptionHandler.handleBusiness(exception)  // @RestControllerAdvice 自动拦截
    → HTTP 422 + RFC 9457 JSON
```

## 1. Domain — 产生异常

```java
// domain/payment/model/Payment.java（节选）
public void charge() {
    if (status != PaymentStatus.PENDING) {
        throw new BusinessException("payment:err.status.pending");
    }
    this.status = PaymentStatus.PAID;
}
```

### 携带参数的异常

```java
// domain/inventory/model/Inventory.java（节选）
public void deductStock(int quantity) {
    if (stock < quantity) {
        throw new BusinessException("inventory:err.insufficientStock",
                Map.of("available", stock, "required", quantity));
    }
    this.stock -= quantity;
}
```

## 2. 错误码命名规范

格式：`"{aggregate}:err.{scene}"`

| 示例 | 含义 |
|------|------|
| `payment:err.notFound` | 支付单不存在 |
| `payment:err.status.pending` | 要求支付单处于 PENDING 状态 |
| `payment:err.status.refundable` | 当前状态不允许退款 |
| `payment:err.amountPositive` | 支付金额必须为正 |
| `inventory:err.insufficientStock` | 库存不足 |
| `inventory:err.priceRequired` | 单价缺失 |
| `inventory:err.priceNegative` | 单价不允许为负 |

规则：
- 全小写，驼峰用 `.` 分隔
- 第一段为聚合名（与包名一致）
- `err.` 固定前缀
- 场景名简洁表达"期望什么"或"出了什么问题"

> 真实例：示例应用的两个聚合实际使用前缀 order 与 product（形如 `<聚合名>:err.<场景>`），见 sample-application 的 domain model 源码（真实例映射位）。

## 3. Infrastructure — 全局异常翻译

```java
// common-exception 模块（框架代码，业务服务无需编写）
// GlobalRestExceptionHandler 由 common-exception 自带的 ExceptionAutoConfiguration
// （META-INF/spring/…AutoConfiguration.imports）注册——引入 common-exception（sample 直接依赖）
// 即在 Servlet Web 环境生效，与 common-cloud 无关
```

### 异常类型 → HTTP 状态码映射

完整映射表（含 400 族 / 404 / 405 / 415 等框架客户端异常全部行）canonical 见 [docs/common/common-exception.md](../../common/common-exception.md) §2 与 `GlobalRestExceptionHandler` 类 javadoc「框架客户端异常显式映射表」，本文不复制。核心通道（与上文传播链一致）：

- `BusinessException`（含状态机守卫失败等**领域规则违反**）→ 缺省 **422**（异常可显式携带其他状态码），`detail` = i18n messageKey
- `IllegalStateException`（乐观锁冲突 `OptimisticLockConflictException` 为其子类，按 IS-A 命中本通道）→ **409**，`detail` 为稳定泛化文案
- 参数校验 / 绑定 / 类型转换失败（`@Valid` 族：BindException / ConstraintViolationException / 类型不匹配等）→ **400**（附 `fieldErrors` 扩展成员）
- `SilentWriteLossException`（INSERT/DELETE 影响 0 行的写丢失级不可能状态）→ **500 + ERROR 日志**，`detail` 固定 "Internal Server Error" 不泄漏内部信息；**独立告警通道，勿与 409 状态冲突混同**——重试无意义、必须吵醒运维（该 handler 刻意置于 ISE 之前以显式分界）
- 其余未捕获异常 → **500** 兜底（泛化标题，不泄内部信息）

## 4. 前端收到的 HTTP 响应

### 基本错误（仅 messageKey）

```http
HTTP/1.1 422 Unprocessable Entity
Content-Type: application/problem+json

{
  "type": "about:blank",
  "title": "Business Error",
  "status": 422,
  "detail": "payment:err.status.pending",
  "instance": "/api/payments/550e8400-e29b-41d4-a716-446655440000/charge"
}
```

### 携带参数的错误

```http
HTTP/1.1 422 Unprocessable Entity
Content-Type: application/problem+json

{
  "type": "about:blank",
  "title": "Business Error",
  "status": 422,
  "detail": "inventory:err.insufficientStock",
  "instance": "/api/checkout",
  "params": { "available": 3, "required": 10 }
}
```

### 乐观锁冲突

```http
HTTP/1.1 409 Conflict
Content-Type: application/problem+json

{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Conflict",
  "instance": "/api/payments/550e8400-e29b-41d4-a716-446655440000/charge"
}
```

> 技术类异常的 `detail` 为稳定泛化文案（防内部实体 ID / SQL 片段外泄）；原始冲突消息只进服务端日志。识别乐观锁冲突请依赖**异常类型**（`OptimisticLockConflictException`），见 [optimistic-lock-retry.md](optimistic-lock-retry.md)。

### 静默写丢失（SilentWriteLossException）

```http
HTTP/1.1 500 Internal Server Error
Content-Type: application/problem+json

{
  "type": "about:blank",
  "title": "Internal Server Error",
  "status": 500,
  "detail": "Internal Server Error",
  "instance": "/api/payments/550e8400-e29b-41d4-a716-446655440000/charge"
}
```

> INSERT/DELETE 影响 0 行的不可能状态：服务端打 **ERROR 级日志（带栈，运维告警抓取信号）**，响应只回固定泛化文案；前端按通用 5xx 处理，**不要重试**（区别于 409 的可重试语义）。

## 5. 前端对接说明

```javascript
// 前端 i18n 渲染示例
const { detail, params } = response.body;
const message = t(detail, params);  // i18next: t("payment:err.status.pending")
// → "当前状态不允许扣款"（由前端 i18n 资源文件定义）
```

- `detail`：仅 **BusinessException 通道**为 i18n 位点（messageKey），前端用 `t(key, params)` 渲染本地化文案；校验族 / 技术异常 / 500 通道的 `detail` 为固定泛化文案（不可作 i18n key 渲染）
- `params`：占位符参数，仅 BusinessException 携带非空参数时出现，可选
- `fieldErrors`：400 校验/绑定族独有扩展成员（字段名 → 校验消息）
- 前端**不解析** title / status（仅用于日志和监控）

## 安全注意

> **禁止**在 `params` 中放入敏感信息（密码、Token、内部 ID 映射表、SQL 语句等）。
> `params` 内容会被完整序列化到 HTTP 响应体，对客户端可见。

## 完整文件清单（异常链路涉及）

| 层 | 文件 | 职责 |
|----|------|------|
| domain | `model/Payment.java`（虚构教例） | 显式 if-throw 产生 BusinessException |
| application | `handler/command/ChargePaymentHandler.java`（虚构教例） | 异常向上传播（不 catch） |
| framework | `common-exception/GlobalRestExceptionHandler` | SPI 自动翻译为 HTTP 响应 |
| contract | — | 无（异常不经过 contract） |
