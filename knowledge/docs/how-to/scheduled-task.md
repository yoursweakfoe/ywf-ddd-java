# 定时任务 · 设计卡

> **本篇=设计卡（2026-09-06 统一用法归卷裁定）**：只回答"该不该用、怎么选"。一切形状、代码、选型表与落地模板见法卷 → [../../specs/current/patterns/scheduler.md](../../specs/current/patterns/scheduler.md)。

> 设计原理 → [../explanation/adapter.md](../explanation/adapter.md) ｜ 同类入口参照 → [write-path.md](write-path.md)（web 入口）

## 什么时候需要时间驱动入口

周期性批量收敛（自动完成超时未确认、对账、清理）。Scheduler 是 adapter 层框架内置的 driving adapter 之一，与 RestAdapter（HTTP）平级——时间只是又一种触发源，下游链路与各入口完全一致（法卷 SC-7）。为什么值得独立成类入口：触发逻辑散写在 Controller / 各处业务代码里则无法被架构规则（R14a/R14b）定位与约束，批量编排与业务规则混杂；独立 Scheduler 让时间触发集中一处可见。

## 四个设计决策点

1. **自建还是平台化**：单实例/少量任务用自建 @Scheduled；多实例/任务多/要运维面板用 XXL-Job / ElasticJob / Quartz 等平台。选型表在册（法卷 §2.8）。标记与规则对两者一视同仁——换触发注解对 R14 影响为零（法卷 SC-5）。
2. **多实例重复执行谁防**：自建模式每实例独立触发，需分布式锁（ShedLock 等，业务自理）+ 业务状态守卫，缺一不可（法卷 SC-4）；平台化天然规避重复触发，但失败重试**不是**可重入豁免（法卷 SC-8）。
3. **批量编排放哪**：Scheduler 纯透传（法卷 SC-2），编排在 Handler 单事务内：条件查询 → 聚合行为 × N → 基类批量落库；超大批量自行分片（法卷 SC-6，BW-5 互指）。
4. **时间从哪来**：一律注入 Clock，禁止裸调 now()——与审计填充同一时间源、可测试可冻结（法卷 SC-3，ADR-0006 判例）。

## 边界与代价

- 批量=单事务逐条循环，耗时线性于体量——分片义务由此而来 → [batch-operations.md](batch-operations.md)
- 联动其他聚合与写路径完全一致（Handler / DomainService 同事务直调），调度入口无特权下游
- 决策型读需追加 Repository 子接口具名方法（读端口配对义务，法卷 SC-6）

## 落地状态

一律以法卷 §3 生效登记为准（框架标记 + R14 守护 ✅ / 示例调度器件 ⛔ 未落地——真实例映射落位清单与本卷 §2 落地模板在册）。
