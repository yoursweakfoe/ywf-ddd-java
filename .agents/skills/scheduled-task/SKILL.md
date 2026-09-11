---
name: scheduled-task
description: 为已有聚合新增定时任务入口（adapter 层 Scheduler）。当需要周期性自动执行某业务逻辑时使用。
---

# 新增定时任务

## 前置阅读

- `knowledge/specs/current/patterns/chain/scheduler.md`（定时任务法卷：SC-1~SC-8 条款 + §2 规范形状，施工唯一权威——本技能只载工序，形状不复述）
- `knowledge/docs/how-to/scheduled-task.md`（设计卡：何时需要时间驱动入口、四个设计决策点、边界代价——零形状代码）

## 第 0 步：契约先行（spec-first）

动手实现前，在 `sample-application/specs/changes/<YYYY-MM-slug>/` 立四件套（模板在 `knowledge/specs/changes/_template/`，首案时开册、目录未立属正常）：specify（问题/验收 AC 账/不做/裁决问句）→ plan（技术裁量 + 对 current 的修卷 delta：ADDED/MODIFIED/REMOVED，SHALL+Scenario）→ tasks（纯清单）→ implement（执行账）。测试全绿后归档折叠进 `sample-application/specs/current/<agg>.md`——文档同步义务只在那一刻发生（归属法卷 §4）。

## 步骤（链路与形状唯一权威 → 法卷 §2：全景 2.1、入口 2.3、门面 2.4、编排 2.5、开关 2.6）

1. **adapter**：创建 `adapter/task/scheduler/{Agg}{Action}Scheduler.java`，形状照法卷 §2.3
   - 实现 `ScheduledAdapter` 标记（SC-1：包位置 + 标记双重约束，R14a/R14b 机器强制，漏实现被架构测试拦截）；adapter 层不按聚合分包 → 蓝图卷 §5 服务骨架通式（`knowledge/specs/current/patterns/building-block/aggregate-blueprint.md`）
   - 触发注解按调度模式选：自建 `@Scheduled(cron = "...")`，或平台化 handler 注解（如 `@XxlJob`，变体形状 §2.7）——SC-5：R14 只认「包位置 + 标记」不认触发注解，换注解对规则零影响
   - 构造器注入 `{Agg}AppService`；方法体纯透传（SC-2）：日志开始 → 委托 AppService → 日志结束（含处理数量）
2. **application**：在 `{Agg}AppService` 新增方法——用例门面只委托 Handler（SC-2，形状 §2.4）
3. **application**：创建 `application/{agg}/handler/command/{Action}{Agg}Handler.java`，形状照法卷 §2.5
   - 标注 `@Transactional(rollbackFor = Exception.class)`（R11 机器强制：批量原子性在本层，框架通道方法本身不标注）
   - 时间一律经注入 `Clock` 派生，禁裸调 `OffsetDateTime.now()`（SC-3，与审计填充同一时间源、可测试可冻结）
   - 典型编排：条件查询 → 聚合行为 × N（状态变迁与校验在聚合根内）→ 基类批量通道落库（SC-6：`updateDomainBatch` 为 `MybatisPersistence` 基行为、非 domain Repository 契约成员；超大批量自行分片 → batch-write 卷 BW-5）
   - 需联动其他聚合时 Handler / DomainService 同事务直调，调度入口无特权下游（SC-7 → §2.9，细则 cross-aggregate 卷）
4. **domain**（如需新决策型读）：在 `{Agg}Repository` 子接口按业务命名新增方法签名（SC-6；读端口配对义务 → `knowledge/docs/how-to/new-aggregate.md` ⑭/⑰）
5. **infrastructure**：在 `{Agg}RepositoryImpl` 实现条件查询——具名 Mapper 方法 + 具名 XML 语句，禁 Wrapper 动态条件（禁令卷 §6）
6. **启动类**：自建模式需确认 `@EnableScheduling` 已标注（§2.6；sample 已带则跳过）——平台化 handler 模式无需（SC-5）

## 多实例与幂等（施工前先选型）

- 选型判据 → 设计卡 `knowledge/docs/how-to/scheduled-task.md`「四个设计决策点」；调度模式选型表 → 法卷 §2.8
- 自建模式多实例：每实例独立触发，幂等二件套缺一不可——分布式锁（ShedLock 等，框架不内置、业务自理）+ 业务状态守卫（SC-4）
- 平台化模式：调度中心统一触发天然免重复，但失败重试**不是**可重入豁免，状态机守卫与幂等语义照常承担（SC-8）

## 验证

- [ ] Scheduler 位于 `adapter/task/scheduler/` 且实现 `ScheduledAdapter` 标记（SC-1，R14a/R14b 通过）
- [ ] Scheduler 方法体纯透传 AppService，无业务判断（SC-2）
- [ ] Handler 有 `@Transactional`（R11）；时间经注入 `Clock` 派生（SC-3）
- [ ] 批量落库走基类通道 `updateDomainBatch`，决策型读为子接口具名方法（SC-6）
- [ ] 多实例场景：锁 + 状态守卫已落位（SC-4/SC-8）
- [ ] 自建模式启动类有 `@EnableScheduling`（SC-5/§2.6）
- [ ] 日志记录执行开始/结束/处理数量
- [ ] 编译通过
- [ ] 编码完成跑 `ddd-review` 技能自查（末步内置 check-docs 七校验）
