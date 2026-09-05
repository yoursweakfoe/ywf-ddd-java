---
name: scheduled-task
description: 为已有聚合新增定时任务入口（adapter 层 Scheduler）。当需要周期性自动执行某业务逻辑时使用。
---

# 新增定时任务

## 前置阅读

- `docs/application/cookbook/scheduled-task.md`（完整模板 + 分布式锁提示）
- `.agents/rules/03-coding-conventions.md`（Adapter 层纯透传约定）

## 步骤

1. **adapter**：创建 `adapter/{agg}/task/scheduler/{Agg}{Action}Scheduler.java`
   - **实现 `ScheduledAdapter` 标记**（`com.yoursweakfoe.common.ddd.adapter.task.scheduler.ScheduledAdapter`，
     规则 R14a/R14b 强制；漏实现会被架构测试拦截）
   - `@Component` + `@Scheduled(cron = "...")`（或平台化调度注解，如 `@XxlJob`）
   - 构造器注入 `{Agg}AppService`
   - 方法内：日志开始 → 透传 AppService → 日志结束（含处理数量）
   - **禁止**在 Scheduler 内写业务逻辑
2. **application**：在 `{Agg}AppService` 新增方法
   - 委托对应 Handler
3. **application**：创建 `application/{agg}/handler/command/{Action}{Agg}Handler.java`
   - 标注 `@Transactional(rollbackFor = Exception.class)`（R11）
   - 典型模式：条件查询 → 逐个调用领域行为 → updateDomainBatch
4. **domain**（如需新查询方法）：在 Repository 接口新增方法签名
5. **infrastructure**：在 RepositoryImpl 实现条件查询
6. **启动类**：确认 `@EnableScheduling` 已标注（已有则跳过）

## 分布式环境注意

> 选型 canonical → `docs/application/cookbook/scheduled-task.md` §6「调度模式选型与多实例」：自建 `@Scheduled`（单实例直接用；多实例需业务自行引入分布式锁如 ShedLock，框架不内置）vs 平台化调度（XXL-Job / ElasticJob，调度中心统一触发天然无重复）。触发注解按模式可换（`@Scheduled` / `@XxlJob`），`ScheduledAdapter` 标记与 R14a/R14b 对两者一视同仁。

## 验证

- [ ] Scheduler 位于 `adapter/{agg}/task/scheduler/`
- [ ] 实现 `ScheduledAdapter` 标记（R14a/R14b 通过）
- [ ] 纯透传 AppService（无业务判断）
- [ ] Handler 有 `@Transactional`（R11）
- [ ] 启动类有 `@EnableScheduling`
- [ ] 日志记录执行开始/结束/处理数量
- [ ] 编译通过
