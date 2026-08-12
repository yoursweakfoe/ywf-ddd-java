# 异常全链路

> 设计原理 → [module-design/domain.md](../module-design/domain.md)（异常策略章节）

## 业务场景

延续示例应用的电商场景（参见 [write-path.md](write-path.md) 业务场景节）。

本文展示一个业务异常从领域层产生到前端收到 HTTP 响应的**完整链路**。

**业务规则：**

1. 用户尝试支付一个非 PENDING 状态的订单 → 状态机校验失败
2. 领域层显式抛出 `BusinessException`（携带 i18n 位点）
3. 异常沿调用栈向上传播，由 `GlobalRestExceptionHandler`（`@RestControllerAdvice`）自动翻译为 HTTP 响应
4. 前端收到 RFC 9457 格式的 JSON 错误体，用 `t(messageKey, params)` 渲染本地化文案

## 异常传播链路

```
Order.pay()
  → if (status != PENDING) throw new BusinessException("order:err.status.pending")

PayOrderHandler.handle(command)
  → order.pay()  // 异常向上传播（Handler 不 catch）

OrderAppService.payOrder(command)
  → payOrderHandler.handle(command)  // 继续传播

OrderController.payOrder(orderId)
  → orderAppService.payOrder(command)  // 继续传播

Spring MVC 异常解析管线
  → GlobalRestExceptionHandler.handleBusiness(exception)  // @RestControllerAdvice 自动拦截
    → HTTP 422 + RFC 9457 JSON
```

## 1. Domain — 产生异常

```java
// domain/order/model/Order.java（节选）
public void pay() {
    if (status != OrderStatus.PENDING) {
        throw new BusinessException("order:err.status.pending");
    }
    this.status = OrderStatus.PAID;
    registerEvent(new OrderPaidEvent(id));
}
```

### 携带参数的异常

```java
// domain/product/model/Product.java（节选）
public void deductStock(int quantity) {
    if (stock < quantity) {
        throw new BusinessException("product:err.insufficientStock",
                Map.of("available", stock, "required", quantity));
    }
    this.stock -= quantity;
}
```

## 2. 错误码命名规范

格式：`"{aggregate}:err.{scene}"`

| 示例 | 含义 |
|------|------|
| `order:err.notFound` | 订单不存在 |
| `order:err.status.pending` | 要求订单处于 PENDING 状态 |
| `order:err.status.cancellable` | 当前状态不允许取消 |
| `product:err.insufficientStock` | 库存不足 |
| `payment:err.amountPositive` | 支付金额必须为正 |

规则：
- 全小写，驼峰用 `.` 分隔
- 第一段为聚合名（与包名一致）
- `err.` 固定前缀
- 场景名简洁表达"期望什么"或"出了什么问题"

## 3. Infrastructure — 全局异常翻译

```java
// common-exception 模块（框架代码，业务服务无需编写）
// GlobalRestExceptionHandler 通过 ExceptionAutoConfiguration 自动注册，引入 common-cloud 即生效
```

### 异常类型 → HTTP 状态码映射

| 异常类型 | HTTP 状态码 | title | 触发场景 |
|---------|:-----------:|-------|---------|
| `BusinessException` | 422 | Business Error | 业务规则违反 |
| `ConstraintViolationException` | 400 | Validation Failed | 参数校验失败（@Valid） |
| `IllegalStateException` | 409 | Conflict | 乐观锁冲突、状态机非法转换 |
| `IllegalArgumentException` | 400 | Bad Request | 非法参数 |
| 其他未捕获异常 | 500 | Internal Server Error | 系统错误 |

## 4. 前端收到的 HTTP 响应

### 基本错误（仅 messageKey）

```http
HTTP/1.1 422 Unprocessable Entity
Content-Type: application/problem+json

{
  "type": "about:blank",
  "title": "Business Error",
  "status": 422,
  "detail": "order:err.status.pending",
  "instance": "/api/orders/550e8400-e29b-41d4-a716-446655440000/pay"
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
  "detail": "product:err.insufficientStock",
  "instance": "/api/orders",
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
  "detail": "UPDATE affected 0 rows for entity ID: xxx (possible cause: concurrent modification or entity not found)",
  "instance": "/api/orders/550e8400-e29b-41d4-a716-446655440000/pay"
}
```

## 5. 前端对接说明

```javascript
// 前端 i18n 渲染示例
const { detail, params } = response.body;
const message = t(detail, params);  // i18next: t("order:err.status.pending")
// → "订单当前状态不允许支付"（由前端 i18n 资源文件定义）
```

- `detail` 为 i18n 位点（messageKey），前端通过 `t(key, params)` 渲染本地化文案
- `params` 为占位符参数，可选
- 前端**不解析** title / status（仅用于日志和监控）

## 安全注意

> **禁止**在 `params` 中放入敏感信息（密码、Token、内部 ID 映射表、SQL 语句等）。
> `params` 内容会被完整序列化到 HTTP 响应体，对客户端可见。

## 完整文件清单（异常链路涉及）

| 层 | 文件 | 职责 |
|----|------|------|
| domain | `model/Order.java` | 显式 if-throw 产生 BusinessException |
| application | `handler/PayOrderHandler.java` | 异常向上传播（不 catch） |
| framework | `common-exception/GlobalRestExceptionHandler` | SPI 自动翻译为 HTTP 响应 |
| contract | — | 无（异常不经过 contract） |
