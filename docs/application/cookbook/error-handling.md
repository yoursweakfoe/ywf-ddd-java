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
| `product:err.priceRequired` | 商品单价缺失 |
| `product:err.priceNegative` | 商品单价不允许为负 |
| `payment:err.amountPositive` | 支付金额必须为正 |

规则：
- 全小写，驼峰用 `.` 分隔
- 第一段为聚合名（与包名一致）
- `err.` 固定前缀
- 场景名简洁表达"期望什么"或"出了什么问题"

## 3. Infrastructure — 全局异常翻译

```java
// common-exception 模块（框架代码，业务服务无需编写）
// GlobalRestExceptionHandler 由 common-exception 自带的 ExceptionAutoConfiguration
// （META-INF/spring/…AutoConfiguration.imports）注册——引入 common-exception（sample 直接依赖）
// 即在 Servlet Web 环境生效，与 common-cloud 无关
```

### 异常类型 → HTTP 状态码映射

完整映射表（BusinessException / 校验族 / IllegalStateException / 类型转换 / 其他未捕获异常等全部行）canonical 见 [docs/common/common-exception.md](../../common/common-exception.md) §2，本文不复制。核心三条，与上文传播链一致：

- `BusinessException`（含状态机守卫失败等**领域规则违反**）→ 缺省 **422**（异常可显式携带其他状态码），`detail` = i18n messageKey
- `IllegalStateException`（乐观锁冲突 `OptimisticLockConflictException` 即属此类）→ **409**
- 参数校验 / 绑定 / 类型转换失败（`@Valid` 族）→ **400**

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
  "detail": "Conflict",
  "instance": "/api/orders/550e8400-e29b-41d4-a716-446655440000/pay"
}
```

> 技术类异常的 `detail` 为稳定泛化文案（防内部实体 ID / SQL 片段外泄）；原始冲突消息只进服务端日志。识别乐观锁冲突请依赖**异常类型**（`OptimisticLockConflictException`），见 [optimistic-lock-retry.md](optimistic-lock-retry.md)。

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
| application | `handler/command/PayOrderHandler.java` | 异常向上传播（不 catch） |
| framework | `common-exception/GlobalRestExceptionHandler` | SPI 自动翻译为 HTTP 响应 |
| contract | — | 无（异常不经过 contract） |
