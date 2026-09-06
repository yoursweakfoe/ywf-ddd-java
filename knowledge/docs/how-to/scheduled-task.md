# 定时任务全链路

> **宽松件（宽严双份，2026-09-06 裁定）**：本篇=任务菜谱与教学走查。其用法规范条款已入法卷 → [../../specs/current/patterns/scheduler.md](../../specs/current/patterns/scheduler.md)；条款冲突以法卷为准（rules/05 §2），本篇代码为教学全套。

> 设计原理 → [module-design/adapter.md](../explanation/adapter.md)
> 同类入口参照 → [write-path.md](write-path.md)（web 入口）

## 业务场景

延续示例应用的电商场景（参见 [write-path.md](write-path.md) 业务场景节）。

本文以 **"自动完成超时未确认订单"** 为案例，完整展示时间驱动入口的标记、触发、透传与批量编排。下文代码块为 `{Agg}` 通式教学模板；在示例应用的具体落位名（OrderAutoDeliverScheduler 等，真实例映射）以「实现状态」表与文末文件清单为准。

**为什么定时任务是独立的一类入口？**

| 如果把调度逻辑写在别处 | 独立 Scheduler 入口 |
|---|---|
| 触发逻辑散落在 Controller / 各处业务代码里 | 时间触发集中在 `@Scheduled` 方法，一处可见 |
| 无法被架构规则定位与约束 | `ScheduledAdapter` 标记 + ArchUnit R14a/R14b 守护 |
| 批量编排与业务规则混杂 | 纯透传 AppService，批量编排在 Handler |

## 两类入口对照

adapter 层框架内置两类 driving adapter（协议适配入口），Scheduler 是其中由**时间**驱动的一类：

| 入口标记 | 驱动源 | 包位置 | 架构规则 |
|---------|--------|--------|---------|
| `RestAdapter` | HTTP 请求 | `adapter/rest/controller` | R8a / R8b |
| **`ScheduledAdapter`** | **时间类调度（自建 @Scheduled 或 XXL-Job / Quartz 等平台化）** | **`adapter/task/scheduler`** | **R14a / R14b** |

> MQ 消费类入口按同一「协议伞 / 角色」惯例落位，与 rest / task 同构、纯透传。

## 链路全景

```
Spring @Scheduled 触发（cron 到点）
  → {Agg}AutoDeliverScheduler.autoDeliverExpired()       ① 时间驱动入口（纯透传）
    → {Agg}AppService.autoDeliverExpired()               ② 用例门面（委托 Handler）
      → AutoDeliverExpiredHandler.handle()               ③ 批量编排（@Transactional）
        → {agg}Repository.findShippedBefore(threshold)   ④ 条件查询（超时实体，业务子接口具名方法）
        → {agg}.deliver() × N                            ⑤ 聚合行为（状态机变迁）
        → RepositoryImpl 继承基类 updateDomainBatch      ⑥ 批量落库（MybatisPersistence 基行为，逐条 validate）
```

## 实现状态

> 各环节在当前示例应用 / 框架中的落地情况；「未实现」的环节待业务需要时按本文模板补全。

| 环节 | 状态 | 落地位置 |
|------|------|---------|
| `ScheduledAdapter` 标记接口 | ✅ 已实现 | `common-ddd/adapter/task/scheduler/ScheduledAdapter.java` |
| 架构守护规则（R14a/R14b） | ✅ 已实现 | common-test `DddArchitectureRules` + 双端架构测试挂载 |
| 示例实现（OrderAutoDeliverScheduler 等，真实例映射） | ⛔ 未落地 | 本文即落地模板；多实例分布式锁见文末注意事项 |

## 1. Adapter — Scheduler 入口

```java
// adapter/task/scheduler/{Agg}AutoDeliverScheduler.java
package com.yoursweakfoe.sampleapplication.sampleservice.adapter.task.scheduler;

import com.yoursweakfoe.common.ddd.adapter.task.scheduler.ScheduledAdapter;
import com.yoursweakfoe.sampleapplication.sampleservice.application.{agg}.service.{Agg}AppService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 聚合自动交付定时任务 —— 每天凌晨 2 点执行。
 */
@Slf4j
@Component
public class {Agg}AutoDeliverScheduler implements ScheduledAdapter {   // ← R14b：包下类必须实现标记

    private final {Agg}AppService {agg}AppService;

    public {Agg}AutoDeliverScheduler({Agg}AppService {agg}AppService) {
        this.{agg}AppService = {agg}AppService;
    }

    /** 每天凌晨 2:00 执行（cron 表达式：秒 分 时 日 月 周）。 */
    @Scheduled(cron = "0 0 2 * * ?")
    public void autoDeliverExpired() {
        log.info("Starting auto-deliver for expired items");
        int count = {agg}AppService.autoDeliverExpired();
        log.info("Auto-deliver completed: {} items processed", count);
    }
}
```

要点：
- 位于 `adapter/task/scheduler/`，**实现 `ScheduledAdapter` 标记**（R14a：标记类必须在 adapter 层；R14b：包下类必须带标记）
- 触发注解按调度模式选择：自建 `@Scheduled`（Spring 原生，无需额外依赖）或平台化 handler 注解（如 XXL-Job 的 `@XxlJob`）——标记与规则对两者一视同仁
- **纯透传** AppService，不含业务逻辑
- 日志记录执行开始/结束（运维可观测）

## 2. Application — AppService 方法

```java
// application/{agg}/service/{Agg}AppService.java（节选）
public int autoDeliverExpired() {
    return autoDeliverExpiredHandler.handle();
}
```

## 3. Application — Handler

```java
// application/{agg}/handler/command/AutoDeliverExpiredHandler.java
@Component
public class AutoDeliverExpiredHandler {

    private final {Agg}Repository {agg}Repository;
    private final Clock clock;                    // 框架统一时间源（ClockAutoConfiguration 提供，ADR-0006）

    public AutoDeliverExpiredHandler({Agg}Repository {agg}Repository, Clock clock) {
        this.{agg}Repository = {agg}Repository;
        this.clock = clock;
    }

    @Transactional(rollbackFor = Exception.class)
    public int handle() {
        // 时间一律经注入 Clock 派生，禁止裸调 OffsetDateTime.now()（与审计填充同一时间源，可测试可冻结）
        OffsetDateTime threshold = OffsetDateTime.now(clock).minusDays(15);
        List<{Agg}> expired = {agg}Repository.findShippedBefore(threshold);

        expired.forEach(item -> {                 // 聚合行为：deliver() 状态机守卫
            item.deliver();
        });
        {agg}Repository.updateDomainBatch(expired);   // 基类批量行为（见下文说明），逐条 validate

        return expired.size();
    }
}
```

要点：
- `updateDomainBatch` 属 `MybatisPersistence` 基类（`RepositoryImpl` 继承后暴露的基行为），**不是** domain `Repository` 五方法生命周期契约的成员；批量原子性由 Handler 的 `@Transactional` 保证（框架方法本身不标注）
- `findShippedBefore` 是决策型读，按业务命名追加在 `{Agg}Repository` 子接口上、由具名 Mapper 方法实现（契约见 [new-aggregate.md](new-aggregate.md) ⑭/⑰）
- 批量体量注意：单事务逐条循环，超大批量调用方自行分片（≤500 条/批，消费契约见 [batch-operations.md](batch-operations.md) §3）

## 4. 启用定时任务

```java
// Application.java（启动类）
@SpringBootApplication
@EnableScheduling  // ← 启用 @Scheduled 支持
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## 下游协调

Handler 内的聚合行为方法（如 `{agg}.deliver()`）只做状态变迁与校验；需要联动其他聚合时由
Handler / DomainService **同事务直调**——
**时间只是又一种触发源，下游与各入口完全一致**。

## 5. 平台化调度变体（XXL-Job / ElasticJob / Quartz）

平台化调度下**仅触发注解不同**，标记、透传与下游链路完全一致：

```java
// XXL-Job 变体
@Slf4j
@Component
public class {Agg}AutoDeliverScheduler implements ScheduledAdapter {   // 标记不变

    @XxlJob("{agg}AutoDeliverHandler")                               // 触发注解换平台
    public void autoDeliverExpired() {
        log.info("Starting auto-deliver for expired items");
        int count = {agg}AppService.autoDeliverExpired();
        log.info("Auto-deliver completed: {} items processed", count);
    }
}
```

对 `ScheduledAdapter` 标记与 R14a/R14b 的影响：**零**——规则只认「包位置 + 标记」，
不认触发注解。

## 6. 调度模式选型与多实例

| 维度 | 自建 `@Scheduled` | 平台化调度（XXL-Job / ElasticJob / PowerJob） |
|------|------------------|---------------------------------------------|
| 多实例重复执行 | 每实例独立触发，需分布式锁（ShedLock 等，业务自理） | 天然规避（调度中心统一触发） |
| 失败重试 / 告警 | 自行实现 | 平台内置 |
| 动态调整 cron | 改配置重启 | 调度台在线调整 |
| 额外组件 | 无 | 调度中心 + 执行器依赖 |
| 适用场景 | 单实例 / 少量任务 | 多实例 / 任务多 / 需要运维面板 |

> 幂等责任提示：无论哪种模式，平台的失败重试都**不是**可重入豁免——Handler/聚合侧的
> 状态机守卫与幂等语义照常承担。

## 完整文件清单

> 本表为示例应用具体落位（真实例映射位，⛔ 标记者未落地、按本文模板补全）。

| 层 | 文件 | 职责 |
|----|------|------|
| common-ddd | `adapter/task/scheduler/ScheduledAdapter.java` | 入口角色标记（✅ 框架已备） |
| adapter | `task/scheduler/OrderAutoDeliverScheduler.java` | 定时触发入口（实现标记，⛔ 待落地） |
| application | `service/OrderAppService.java` | 委托 Handler |
| application | `handler/command/AutoDeliverExpiredHandler.java` | 批量编排 |
| domain | `domain/order/repository/OrderRepository.java` | 新增 `findShippedBefore` 具名方法 |
| infrastructure | `infrastructure/persistence/master/order/repository/OrderRepositoryImpl.java` | 条件查询实现 + 基类批量行为 |
