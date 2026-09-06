# 用法规范法卷：定时任务（框架法 · 严格件）

> **身份**：本卷规范 Scheduler 入口形态；修卷走 `../../changes/`。docs 同题篇（`how-to/scheduled-task.md`）为宽松件，冲突以本卷为准。
> **机器对账**：C1/C3/C4 扫本卷。开册法案：`2026-09-howto-codification`。

## 条款

| # | SHALL | 取证 | 背书 |
|---|---|---|---|
| SC-1 | 定时任务入口置于 `adapter/task/scheduler/`，实现 `ScheduledAdapter` 标记接口（R14a 位置约束、R14b 标记约束） | 框架 `ScheduledAdapter`（common-ddd 在库）；ArchUnit | C3 |
| SC-2 | Scheduler 方法体纯透传（委托 AppService/Handler），禁止在调度类内写业务分支——与 Adapter 纯透传同规 | AGENTS 九条 2；WC-4 互指 | ArchUnit |
| SC-3 | 时间判断一律使用注入的 `Clock`，禁止直调 `OffsetDateTime.now()`（统一时间源、可测性） | AGENTS 九条 8；ADR-0006 判例；`modules/ddd.md` Clock 条款 | 测试可注入验证 |
| SC-4 | 多实例部署的调度必须自带幂等：分布式锁（防并发重入）+ 业务状态守卫（防重复效应），二者缺一不可 | 原篇「多实例部署分布式锁」注记入法 | <!-- 锁组件落地度待注：见原篇状态表 --> |

## 生效登记

| 条款 | 状态 |
|---|---|
| SC-1/2/3 | ✅ 形态约束生效（ArchUnit + 框架件在库） |
| sample 落地 | ⛔ 真实例映射登记于原篇状态表（调度器示例件未全量落地，模板为准） |
