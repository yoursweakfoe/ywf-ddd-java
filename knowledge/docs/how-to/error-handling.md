# 异常处理 · 设计卡

> **本篇 = 设计卡（宽松件）**：只回答"异常从哪抛、沿什么路径、前端怎么对接"。传播链、抛出与响应形状、映射通道、前端契约的全部条款见法卷 → [../../specs/current/modules/exception.md](../../specs/current/modules/exception.md)。条款冲突以法卷为准，见归属法卷 §2。本篇零形状复写。

> 设计原理 → [../explanation/domain.md](../explanation/domain.md)（异常策略章节）。

## 错误码登记簿（全仓 `{aggregate}:err.{scene}` key 唯一登记处）

**登记职责**：新增或废弃错误 key，一律先在本表登一行。这项登记簿职责由法卷 EV-2 载明，属宽松件义务：key 的形状与命名规则在法卷 EV-2/EV-6 和 §5.3，本表只登记、不执法。

| 示例 | 含义 |
|------|------|
| `payment:err.notFound` | 支付单不存在 |
| `payment:err.status.pending` | 要求支付单处于 PENDING 状态 |
| `payment:err.status.refundable` | 当前状态不允许退款 |
| `payment:err.amountPositive` | 支付金额必须为正 |
| `inventory:err.insufficientStock` | 库存不足 |
| `inventory:err.priceRequired` | 单价缺失 |
| `inventory:err.priceNegative` | 单价不允许为负 |

> 真实例：示例应用的两个聚合实际使用前缀 order 与 product，形如 `<聚合名>:err.<场景>`。源码见 sample-application 的 domain model，映射位记录在法卷 §5.3。

## 业务场景

> 上文登记簿现存 key 属**虚构教例**：payment 与 inventory 两个聚合，sample 未实现。真实前缀见上注。

下面走一遍完整链路。场景：对非 PENDING 状态的支付单扣款，状态机校验失败。聚合行为方法显式抛 `BusinessException`，携带 i18n 位点。异常沿调用栈向上传播，中途无人 catch。框架 advice 自动把它翻译成 RFC 9457 JSON。前端用 `t(messageKey, params)` 渲染本地化文案。指针：传播链见法卷 EV-7，链路形状见 §5.1。

## 三个设计决策点

1. **在哪抛**：业务规则归聚合行为方法，显式 if-throw，形状见法卷 §5.2。不定义具名领域异常，domain 层也不设 exception 包，见法卷 EV-1。
2. **状态码怎么给**：缺省 422。需要 404/409 等状态时用显式状态构造，形状见法卷场景 1。400 校验族与 500 兜底由框架通道自动命中，业务不自选、不冒充（法卷 EV-8）。
3. **409 还是 500**：乐观锁冲突走 409，可重试，识别靠异常类型，见 [optimistic-lock-retry.md](optimistic-lock-retry.md)。静默写丢失走 500 加 ERROR 日志，它是独立告警通道，前端**禁重试**。见法卷 EV-4，形状 §5.5/§5.6。

## 边界与安全

- 前端只消费 `detail`/`params`/`fieldErrors`。其中只有 BusinessException 通道的 `detail` 可作 i18n key，见法卷 EV-9，形状 §5.7。`title`/`status` 不解析。
- `params` 禁放敏感信息：密码、Token、内部 ID 映射表、SQL 语句。它会完整序列化进响应体（法卷 EV-3）。
- 异常→HTTP 全量映射表的 canon 在 `GlobalRestExceptionHandler` javadoc，字典镜像见 [../reference/api/common-exception.md](../reference/api/common-exception.md) §2。改表必须同 PR 双更，法卷 EV-5。设计原理纵深 → [../explanation/domain.md](../explanation/domain.md)。

## 落地状态

一律以法卷条款与 §5 在册形状为准。框架翻译链与全部 EV 条款 ✅，取证在卷。本篇登记簿 key 是虚构教例，真实例前缀 order / product，见上注。
