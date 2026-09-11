# 模块用法法卷：common-exception（框架法 · 严格件）

> **身份**：本卷是用法规范与规范代码形状的唯一权威（严格件）。消费代码必须遵循本卷；违反本卷就修代码。修改本卷只能走 `../../changes/` 程序。docs 同题节（`reference/api/common-exception.md` §3）是宽松件，只承载语感与指针；两者冲突时以本卷为准。
> **机器对账**：本卷在 check-docs 扫描面内。C1 校验 `{agg}` 模板实例化，C3 校验符号解析，C4 校验教学中立。异常 → HTTP 映射表由 C5 对账，规则见 EV-5。

---

```xml
<dependency>
    <groupId>com.yoursweakfoe</groupId>
    <artifactId>common-exception</artifactId>
</dependency>
```

引入即生效：REST 异常处理器由 `ExceptionAutoConfiguration` 自动注册。

### 场景 1：抛出业务异常

```java
throw new BusinessException("payment:err.notFound");

throw new BusinessException("payment:err.statusPending",
        Map.of("current", "FAILED", "required", "PENDING"));

// 显式指定 HTTP 状态（默认 422）
throw new BusinessException("payment:err.notFound", 404);
throw new BusinessException("payment:err.statusSuccess",
        Map.of("current", "REFUNDED", "required", "SUCCESS"), 409);
```

> **安全注意**：`params` 内容会序列化进 HTTP 响应体，禁止放入敏感信息。

### 场景 2：领域层显式抛出

聚合根内的状态守卫示例。`{Agg}` 是聚合根类名占位符，虚构教例：

```java
public class {Agg} extends AggregateRoot<UUID> {
    public void pay() {
        requireStatus("{aggregate}:err.status.pending", Status.PENDING);
        this.status = Status.PAID;
    }
}
```

## §4 异常全链路条款（EV）

| # | SHALL | 取证 | 背书 |
|---|---|---|---|
| EV-1 | 业务失败一律 `throw new BusinessException(messageKey, params)`。**禁止**定义具名领域异常；`InsufficientStockException` 是在册反面教学例，非真实类，登记于 check-docs C3 白名单。domain 层不设 `exception/` 包 | AGENTS 九条 7；`编码公约卷` 异常策略节 | C3 白名单 |
| EV-2 | messageKey 格式 `{aggregate}:err.{scene}`。它是前端渲染位点，服务端不维护 messages.properties。禁止硬编码可读文案作 key。全仓 key 清单唯一登记处 = `knowledge/docs/how-to/error-handling.md`，这是宽松件的登记职责、非条款 | 归属法卷 §5 | — |
| EV-3 | `params` 序列化进响应体，禁止携带敏感信息：堆栈、Token、内部 ID 映射、SQL 片段等 | 本卷 §3 安全注意 | 评审项 |
| EV-4 | INSERT/DELETE 影响 0 行属于静默写丢失：`SilentWriteLossException` → **500**，`detail` 固定文案，不泄露内部信息。这条通道与 `OptimisticLockConflictException` 的 **409** 冲突通道严格分道：写丢失走告警通道，不得混同 409 | 框架 `GlobalRestExceptionHandler` javadoc；OL-1 互指 | C5 |
| EV-5 | 异常 → HTTP 映射表的 canon = `GlobalRestExceptionHandler` javadoc；`reference/api/common-exception.md` §2 是字典镜像。改表必须同 PR 双更 | 归属法卷 §2 归属表 + §4 同步行 | **C5 对账** |
| EV-6 | messageKey 命名细则：全小写，驼峰段用 `.` 分隔；第一段是聚合名，与包名一致；`err.` 是固定前缀；场景名简洁表达"期望什么"或"出了什么问题" | 形状 §5.3 | 评审项 |
| EV-7 | 业务异常在聚合行为方法内抛出后**沿调用栈向上传播**：Handler / AppService / Adapter 不 catch、不包装。终止翻译为 HTTP 响应只由 `GlobalRestExceptionHandler`（`@RestControllerAdvice`）完成 | 形状 §5.1 | 评审项 |
| EV-8 | 异常 → HTTP 核心映射通道：`BusinessException` 缺省 **422**，可显式携带其他状态码，`detail` = messageKey；`IllegalStateException` → **409** + 稳定泛化文案，`OptimisticLockConflictException` 是其子类、按 IS-A 命中本通道；`@Valid` 族（BindException / ConstraintViolationException / 类型不匹配）→ **400** + `fieldErrors`；其余未捕获异常 → **500** 泛化兜底 | canon = `GlobalRestExceptionHandler` javadoc（EV-5）；形状 §5.5 | C5 对账 |
| EV-9 | 前端渲染契约：只有 **BusinessException 通道**的 `detail` 可作 i18n key，用 `t(key, params)` 渲染。校验族、技术异常、500 通道的 `detail` 是固定泛化文案，禁作 key。`params` 仅 BusinessException 携带非空参数时出现，可选。`fieldErrors` 仅 400 族出现。前端**不解析** `title` / `status`，仅用于日志监控 | 形状 §5.7 | 评审项 |
| EV-10 | 异常不经过 contract 层：全链路 contract 零异常件，错误语义只靠 messageKey 承载 | §5.1 表（contract 行 = 「无」） | 评审项 |

## §5 全链路形状（统一用法唯一样本）

> 本节教例家族为**虚构**：payment、inventory 两个聚合，sample 未实现，本节只教形状。真实前缀指针见 §5.3 末（真实例）。异常从领域层到前端的设计叙事与 key 登记簿，见 docs 设计卡 `../../../docs/how-to/error-handling.md`。

### 5.1 传播链路（EV-7 形状）

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

链路涉及文件（EV-10）：

| 层 | 文件 | 职责 |
|----|------|------|
| domain | `model/Payment.java`（虚构教例） | 在这里显式 if-throw 抛出 BusinessException |
| application | `handler/command/ChargePaymentHandler.java`（虚构教例） | 不 catch，异常继续向上传播 |
| framework | `common-exception/GlobalRestExceptionHandler` | 经 SPI 自动注册，把异常翻译成 HTTP 响应 |
| contract | — | 无，异常不经过 contract |

### 5.2 抛出形状（EV-1 / EV-3）

```java
// domain/payment/model/Payment.java（节选·虚构教例）
public void charge() {
    if (status != PaymentStatus.PENDING) {                          // EV-1：显式 if-throw，不立具名异常
        throw new BusinessException("payment:err.status.pending");  // EV-2/EV-6：key = {aggregate}:err.{scene}
    }
    this.status = PaymentStatus.PAID;
}
```

```java
// domain/inventory/model/Inventory.java（节选·虚构教例）
public void deductStock(int quantity) {
    if (stock < quantity) {
        throw new BusinessException("inventory:err.insufficientStock",   // EV-3：params 完整序列化进响应体，
                Map.of("available", stock, "required", quantity));       //       禁放敏感信息
    }
    this.stock -= quantity;
}
```

### 5.3 key 命名细则（EV-2 / EV-6 形状）

格式：`"{aggregate}:err.{scene}"`

规则：
- 全小写，驼峰用 `.` 分隔
- 第一段为聚合名（与包名一致）
- `err.` 固定前缀
- 场景名简洁表达"期望什么"或"出了什么问题"

全仓 key 清单**不**登记在本卷。唯一登记处是 docs 设计卡，判据见 EV-2。

> 真实例：示例应用的两个聚合实际使用前缀 order 与 product，形如 `<聚合名>:err.<场景>`；源码见 sample-application 的 domain model（真实例映射位）。

### 5.4 框架注册（EV-5 前提）

```java
// common-exception 模块（框架代码，业务服务无需编写）
// GlobalRestExceptionHandler 由 common-exception 自带的 ExceptionAutoConfiguration
// （META-INF/spring/…AutoConfiguration.imports）注册——引入 common-exception（sample 直接依赖）
// 即在 Servlet Web 环境生效，与 common-cloud 无关
```

### 5.5 核心映射通道（EV-8 叙事；EV-4 独立通道）

完整映射表本卷不复制。它的两处权威：canon = `GlobalRestExceptionHandler` 类 javadoc「框架客户端异常显式映射表」，字典镜像 = [../../docs/reference/api/common-exception.md](../../../docs/reference/api/common-exception.md) §2。表中含 400 族、404、405、415 等框架客户端异常的全部行。下面是与 §5.1 传播链一致的核心通道：

- `BusinessException`（含状态机守卫失败等**领域规则违反**）→ 缺省 **422**；异常可显式携带其他状态码；`detail` = i18n messageKey
- `IllegalStateException` → **409**，`detail` 为稳定泛化文案；`OptimisticLockConflictException` 是其子类，按 IS-A 命中本通道
- 参数校验、绑定、类型转换失败 → **400**（`@Valid` 族：BindException / ConstraintViolationException / 类型不匹配等），附 `fieldErrors` 扩展成员
- `SilentWriteLossException`（INSERT/DELETE 影响 0 行的写丢失级不可能状态）→ **500 + ERROR 日志**，`detail` 固定 "Internal Server Error" 不泄漏内部信息。它是**独立告警通道，勿与 409 状态冲突混同**：重试无意义，必须吵醒运维。该 handler 刻意排在 `IllegalStateException` 的 handler 之前，以显式分界
- 其余未捕获异常 → **500** 兜底，泛化标题，不泄内部信息

### 5.6 响应形状（RFC 9457 问题详情；对应 EV-8）

四例均来自 §5.1 的虚构教例链路。

#### 基本错误（仅 messageKey）

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

#### 携带参数的错误

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

#### 乐观锁冲突

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

> 技术类异常的 `detail` 是稳定泛化文案，防止内部实体 ID、SQL 片段外泄；原始冲突消息只进服务端日志。识别乐观锁冲突要依赖**异常类型**（`OptimisticLockConflictException`），见 [../../docs/how-to/optimistic-lock-retry.md](../../../docs/how-to/optimistic-lock-retry.md)。

#### 静默写丢失（SilentWriteLossException）

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

> INSERT/DELETE 影响 0 行是不可能状态：服务端打**带栈的 ERROR 级日志**，这是运维告警的抓取信号；响应只回固定泛化文案。前端按通用 5xx 处理，**不要重试**——这与 409 的可重试语义不同，重试没有意义。

### 5.7 前端消费形状（EV-9 / EV-3）

```javascript
// 前端 i18n 渲染示例
const { detail, params } = response.body;
const message = t(detail, params);  // EV-9：仅 BusinessException 通道的 detail 可作 key
// i18next: t("payment:err.status.pending")
// → "当前状态不允许扣款"（由前端 i18n 资源文件定义）
```

- `detail`：只有 **BusinessException 通道**是 i18n 位点（messageKey），前端用 `t(key, params)` 渲染本地化文案。校验族、技术异常、500 通道的 `detail` 是固定泛化文案，不可作 i18n key 渲染。
- `params`：占位符参数，仅 BusinessException 携带非空参数时出现，可选字段。
- `fieldErrors`：400 校验/绑定族独有的扩展成员，字段名 → 校验消息。
- 前端**不解析** title / status，它们只用于日志和监控。

> **安全注意（EV-3）**：**禁止**在 `params` 中放入敏感信息：密码、Token、内部 ID 映射表、SQL 语句等。`params` 内容会被完整序列化到 HTTP 响应体，对客户端可见。
