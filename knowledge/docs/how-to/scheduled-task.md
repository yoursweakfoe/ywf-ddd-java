# 定时任务 · 设计卡

> **本篇 = 设计卡（宽松件）**：只回答"该不该用、怎么选"。形状、代码、选型表与落地模板全在法卷 → [../../specs/current/patterns/chain/scheduler.md](../../specs/current/patterns/chain/scheduler.md)。

> 设计原理 → [../explanation/adapter.md](../explanation/adapter.md) ｜ 同类入口参照 → [write-path.md](write-path.md)（web 入口）

## 什么时候需要时间驱动入口

周期性批量收敛时用：自动完成超时未确认、对账、清理。

Scheduler 是 adapter 层框架内置的 driving adapter 之一，与 RestAdapter（HTTP）平级。时间只是又一种触发源，下游链路与其它入口完全一致，见法卷 SC-7。

为什么要独立成一类入口：触发逻辑散写在 Controller 或各处业务代码里，架构规则 R14a/R14b 定位不到、也约束不住，批量编排还会与业务规则混在一起。独立 Scheduler 让时间触发集中在一处，看得见。

## 四个设计决策点

1. **自建还是平台化**：单实例、任务少，用自建 @Scheduled。多实例、任务多、要运维面板，用 XXL-Job / ElasticJob / Quartz 这类平台。选型表在册，见法卷 §2.8。标记与规则对两者一视同仁：换触发注解，对 R14 的影响为零（法卷 SC-5）。
2. **多实例重复执行谁防**：自建模式下每个实例独立触发，所以要分布式锁，再加业务状态守卫，两者缺一不可，见法卷 SC-4。锁的选型由业务自理，ShedLock 之类都行。平台化天然规避重复触发，但它的失败重试**不是**可重入豁免，见法卷 SC-8。
3. **批量编排放哪**：Scheduler 纯透传，见法卷 SC-2。编排在 Handler 的单事务内：条件查询 → 聚合行为 × N → 基类批量落库。超大批量自行分片，见法卷 SC-6，该条与 BW-5 互指。
4. **时间从哪来**：一律注入 Clock，禁止裸调 now()。这样取的是与审计填充同一个时间源，可测试、可冻结。条款见法卷 SC-3。

## 边界与代价

- 批量 = 单事务逐条循环，耗时线性于体量，分片义务由此而来 → [batch-operations.md](batch-operations.md)。
- 联动其他聚合与写路径完全一致：Handler / DomainService 同事务直调。调度入口没有特权下游。
- 决策型读要给 Repository 子接口追加具名方法，这是读端口配对义务，见法卷 SC-6。

## 落地状态

一律以法卷 §3 生效登记为准。框架标记 + R14 守护 ✅；示例调度器件 ⛔ 未落地，真实例映射落位清单与本卷 §2 落地模板在册。
