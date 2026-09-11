# 乐观锁冲突与重试 · 设计卡

> **本篇 = 设计卡（宽松件）**：只回答"该不该用、怎么选"。形状与代码（传播路径/重试模板/策略表/409 契约）全在法卷 → [../../specs/current/patterns/optimistic-lock.md](../../specs/current/patterns/optimistic-lock.md)。

> 设计原理 → [../explanation/infrastructure.md](../explanation/infrastructure.md) ｜ 分类契约字典 → [../reference/api/common-ddd.md](../reference/api/common-ddd.md) §2「持久化支撑（MybatisPersistence）」

## 什么时候需要关心冲突

两个请求并发改同一个聚合实例时。案例教例："两人同时对同一支付单扣款"，只有一个能成功。

框架默认行为已经把答案给完了：UPDATE 带版本条件、命中 0 行且实体仍在 → 抛冲突异常 → 自动映射 HTTP 409 Conflict。失败方收到明确错误，不是静默丢失。

**用户交互场景到此为止，不需要任何代码。**前端提示"操作冲突，请刷新重试"就够了。需要额外动作的只有系统间调用，也就是 MQ Consumer 和定时任务。

## 三个设计决策点

1. **重试还是甩锅**：REST 前端不重试，直接回 409。MQ 消费靠状态机幂等天然消化，重复消费遇到已 PAID 就忽略。定时任务和系统间调用自动重试。高并发秒杀是乐观锁加重试加网关限流的组合。策略选择表在册，见法卷 §2.2。
2. **重试包在哪一层**：包在应用层 Handler 的包装器里，模板见法卷 §2.4。形状是指数退避、有界次数、耗尽上抛（法卷 OL-5）。重试属编排关注点，聚合根内禁止重试，见 OL-6。每次重试重新 load，拿最新 version，见 OL-3。真实例：示例应用 PlaceOrder 链路 RetryablePlaceOrderHandler 已落地，见法卷 §3。
3. **哪些异常绝不进重试器**：只 catch 冲突类型。实体消失的普通 IllegalStateException、INSERT/DELETE 命中 0 行的 SilentWriteLossException（500+ERROR），一律直接上抛。重试对它们没有意义，必须让告警吵醒人。见法卷 OL-7 的三分通道围栏。

## 边界与代价

- 识别冲突只走编译期类型通道，新代码禁止依赖消息文本做语义判断，见法卷 OL-2。affected 0 rows 字样只在框架侧兼容期过渡使用。
- 版本号由框架 SQL 维护，业务层禁手工读写，见法卷 OL-4。版本条件由手写 XML 的 UPDATE 语句文本自身携带，没有运行时拦截器，框架之外无需感知。
- 批量里单条冲突会整批回滚，重试要整批重载，所以批量慎用重试，见法卷 §2.5 注意表。虚拟线程下 sleep 不占载体线程，等待没有顾虑，语境见 AGENTS 九条第 8 条。

## 落地状态

一律以法卷 §3 生效登记为准。框架冲突/409 通道 OL-1~4 ✅。重试形状 OL-5~7 ✅ 模板在册：虚构 payment 教例模板，加真实例 PlaceOrder 链路件已落地，含 4 单测。
