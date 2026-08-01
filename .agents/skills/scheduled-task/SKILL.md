---
name: scheduled-task
description: 为已有聚合新增定时任务入口（adapter 层 Scheduler）。当需要周期性自动执行某业务逻辑时使用。
---

# 新增定时任务

## 前置阅读

- `docs/sample-application/cookbook/scheduled-task.md`（完整模板 + 分布式锁提示）
- `.agents/rules/03-coding-conventions.md`（Adapter 层纯透传约定）

## 步骤

1. **adapter**：创建 `adapter/{agg}/scheduler/{Agg}{Action}Scheduler.java`
   - `@Component` + `@Scheduled(cron = "...")`
   - 构造器注入 `{Agg}AppService`
   - 方法内：日志开始 → 透传 AppService → 日志结束（含处理数量）
   - **禁止**在 Scheduler 内写业务逻辑
2. **application**：在 `{Agg}AppService` 新增方法
   - 委托对应 Handler
3. **application**：创建 `application/{agg}/handler/{Action}Handler.java`
   - 标注 `@Transactional(rollbackFor = Exception.class)`
   - 典型模式：条件查询 → 逐个调用领域行为 → updateDomainBatch
4. **domain**（如需新查询方法）：在 Repository 接口新增方法签名
5. **infrastructure**：在 RepositoryImpl 实现条件查询
6. **启动类**：确认 `@EnableScheduling` 已标注（已有则跳过）

## 分布式环境注意

| 部署模式 | 策略 |
|---------|------|
| 单实例 | 直接 @Scheduled，无需额外处理 |
| 多实例 | 需分布式锁（ShedLock / Redis SETNX），防止重复执行 |
| 复杂调度 | 考虑 XXL-Job / ElasticJob |

> 框架不内置分布式锁，多实例时由业务自行引入。

## 验证

- [ ] Scheduler 位于 `adapter/{agg}/scheduler/`
- [ ] 纯透传 AppService（无业务判断）
- [ ] Handler 有 `@Transactional`
- [ ] 启动类有 `@EnableScheduling`
- [ ] 日志记录执行开始/结束/处理数量
- [ ] 编译通过
