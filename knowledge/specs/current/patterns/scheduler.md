# 用法规范法卷：定时任务（框架法 · 严格件）

> **身份**：本卷是 Scheduler 入口**统一用法**的唯一权威——全套规范形状在此，全仓他处不得复写形状；违反本卷=修代码，修卷走 `../../changes/`。docs 同题篇（`../../../docs/how-to/scheduled-task.md`）为设计卡（选型与边界叙事，零形状代码），冲突以本卷为准。
> **机器对账**：C1/C3/C4 扫本卷；教例家族 {Agg} 通式（虚构模板；OrderAutoDeliverScheduler 等为真实例映射名，sample 未落地）。开册法案：2026-09 设计卡降格案；统一用法归卷：2026-09 用法归卷案。

## §1 条款

| # | SHALL | 取证 | 背书 |
|---|---|---|---|
| SC-1 | 定时任务入口置于 `adapter/task/scheduler/`，实现 `ScheduledAdapter` 标记接口（R14a 位置约束、R14b 标记约束） | 框架 `ScheduledAdapter`（common-ddd 在库）；ArchUnit | C3 |
| SC-2 | Scheduler 方法体纯透传（委托 AppService/Handler），禁止在调度类内写业务分支——与 Adapter 纯透传同规 | AGENTS 九条 2；WC-4 互指 | ArchUnit |
| SC-3 | 时间判断一律使用注入的 `Clock`，禁止直调 `OffsetDateTime.now()`（统一时间源、可测性） | AGENTS 九条 8；旧案 判例；`modules/ddd.md` Clock 条款 | 测试可注入验证 |
| SC-4 | 多实例部署的调度必须自带幂等：分布式锁（防并发重入）+ 业务状态守卫（防重复效应），二者缺一不可 | 原篇「多实例部署分布式锁」注记入法 | <!-- 锁组件落地度待注：见原篇状态表 --> |
| SC-5 | 触发注解按调度模式选择：自建 `@Scheduled`（Spring 原生、无额外依赖，启动类需 `@EnableScheduling`）或平台化 handler 注解（如 XXL-Job `@XxlJob`）——R14a/R14b 只认「包位置 + 标记」不认触发注解，变体对规则影响为零 | 本卷 §2.3/§2.6/§2.7 形状；原篇 §5「零影响」注记入法 | ArchUnit |
| SC-6 | 定时批量落库一律经 `MybatisPersistence` 基类 `updateDomainBatch`（**非** Repository 五方法生命周期契约成员，内部逐条 validate），原子性由 Handler `@Transactional` 保证；超大批量调用方自行分片（≤500 条/批）；决策型读（如 `findShippedBefore`）按业务命名追加 `{Agg}Repository` 子接口、由具名 Mapper 方法实现 | BW 卷互指（BW-3/BW-4/BW-5）；契约见 `how-to/new-aggregate.md` ⑭/⑰（原篇注） | 守恒测试 |
| SC-7 | 下游协调与各入口完全一致——时间只是又一种触发源：聚合行为只做状态变迁与校验，联动其他聚合由 Handler / DomainService 同事务直调，禁止调度专用下游通道 | 原篇「下游协调」节入法；[cross-aggregate.md](cross-aggregate.md) 卷互指 | — |
| SC-8 | 平台化调度的失败重试**不是**可重入豁免：无论自建或平台模式，Handler/聚合侧的状态机守卫与幂等语义照常承担 | 原篇「幂等责任提示」入法；SC-4 配对 | — |

## §2 规范形状（统一用法唯一样本）

> 本章代码块为 `{Agg}` 通式教学模板（业务无关，虚构教例）；案例为「自动完成超时未确认订单」式的时间驱动入口——在示例应用的具体落位名（OrderAutoDeliverScheduler 等，真实例映射）以 §3 生效登记为准。

### 2.1 链路全景

```
Spring @Scheduled 触发（cron 到点）
  → {Agg}AutoDeliverScheduler.autoDeliverExpired()       ① 时间驱动入口（纯透传）
    → {Agg}AppService.autoDeliverExpired()               ② 用例门面（委托 Handler）
      → AutoDeliverExpiredHandler.handle()               ③ 批量编排（@Transactional）
        → {agg}Repository.findShippedBefore(threshold)   ④ 条件查询（超时实体，业务子接口具名方法）
        → {agg}.deliver() × N                            ⑤ 聚合行为（状态机变迁）
        → RepositoryImpl 继承基类 updateDomainBatch      ⑥ 批量落库（MybatisPersistence 基行为，逐条 validate）
```

### 2.2 两类入口对照

adapter 层框架内置两类 driving adapter（协议适配入口），Scheduler 是其中由**时间**驱动的一类：

| 入口标记 | 驱动源 | 包位置 | 架构规则 |
|---------|--------|--------|---------|
| `RestAdapter` | HTTP 请求 | `adapter/rest/controller` | R8a / R8b |
| **`ScheduledAdapter`** | **时间类调度（自建 @Scheduled 或 XXL-Job / Quartz 等平台化）** | **`adapter/task/scheduler`** | **R14a / R14b** |

> MQ 消费类入口按同一「协议伞 / 角色」惯例落位，与 rest / task 同构、纯透传。

### 2.3 Scheduler 入口（adapter 段，自建 @Scheduled 模式）

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
public class {Agg}AutoDeliverScheduler implements ScheduledAdapter {   // ← R14b：包下类必须实现标记（SC-1；R14a：标记类必须在 adapter 层）

    private final {Agg}AppService {agg}AppService;

    public {Agg}AutoDeliverScheduler({Agg}AppService {agg}AppService) {
        this.{agg}AppService = {agg}AppService;
    }

    /** 每天凌晨 2:00 执行（cron 表达式：秒 分 时 日 月 周）。 */
    @Scheduled(cron = "0 0 2 * * ?")
    public void autoDeliverExpired() {
        log.info("Starting auto-deliver for expired items");           // 日志记录执行开始/结束（运维可观测）
        int count = {agg}AppService.autoDeliverExpired();               // SC-2：纯透传 AppService，不含业务逻辑
        log.info("Auto-deliver completed: {} items processed", count);
    }
}
```

### 2.4 AppService 方法（application 段 · 节选）

```java
// application/{agg}/service/{Agg}AppService.java（节选）
public int autoDeliverExpired() {
    return autoDeliverExpiredHandler.handle();      // 用例门面（委托 Handler）
}
```

### 2.5 Handler 批量编排（application 段）

```java
// application/{agg}/handler/command/AutoDeliverExpiredHandler.java
@Component
public class AutoDeliverExpiredHandler {

    private final {Agg}Repository {agg}Repository;
    private final Clock clock;                    // 框架统一时间源（ClockAutoConfiguration 提供，旧案）

    public AutoDeliverExpiredHandler({Agg}Repository {agg}Repository, Clock clock) {
        this.{agg}Repository = {agg}Repository;
        this.clock = clock;
    }

    @Transactional(rollbackFor = Exception.class)  // SC-6：批量原子性在此层，框架通道方法本身不标注
    public int handle() {
        // SC-3：时间一律经注入 Clock 派生，禁止裸调 OffsetDateTime.now()（与审计填充同一时间源，可测试可冻结）
        OffsetDateTime threshold = OffsetDateTime.now(clock).minusDays(15);
        List<{Agg}> expired = {agg}Repository.findShippedBefore(threshold);   // SC-6：决策型读，子接口具名方法

        expired.forEach(item -> {                 // 聚合行为：deliver() 状态机守卫
            item.deliver();
        });
        {agg}Repository.updateDomainBatch(expired);   // SC-6/BW-4：基类批量行为，逐条 validate

        return expired.size();
    }
}
```

要点（形状注记）：

- `updateDomainBatch` 属 `MybatisPersistence` 基类（`RepositoryImpl` 继承后暴露的基行为），**不是** domain `Repository` 五方法生命周期契约的成员；批量原子性由 Handler 的 `@Transactional` 保证（框架方法本身不标注）
- `findShippedBefore` 是决策型读，按业务命名追加在 `{Agg}Repository` 子接口上、由具名 Mapper 方法实现（契约见 `how-to/new-aggregate.md` ⑭/⑰）
- 批量体量注意：单事务逐条循环，超大批量调用方自行分片（≤500 条/批，消费契约见法卷 [batch-write.md](batch-write.md) BW-5）

### 2.6 启用定时任务（启动类开关）

```java
// Application.java（启动类）
@SpringBootApplication
@EnableScheduling  // ← 启用 @Scheduled 支持（SC-5：自建模式必需；平台化 handler 模式无需）
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 2.7 平台化调度变体（XXL-Job / ElasticJob / Quartz）

平台化调度下**仅触发注解不同**，标记、透传与下游链路完全一致：

```java
// XXL-Job 变体
@Slf4j
@Component
public class {Agg}AutoDeliverScheduler implements ScheduledAdapter {   // SC-5：标记不变

    @XxlJob("{agg}AutoDeliverHandler")                               // 触发注解换平台
    public void autoDeliverExpired() {
        log.info("Starting auto-deliver for expired items");
        int count = {agg}AppService.autoDeliverExpired();
        log.info("Auto-deliver completed: {} items processed", count);
    }
}
```

对 `ScheduledAdapter` 标记与 R14a/R14b 的影响：**零**——规则只认「包位置 + 标记」，不认触发注解。

### 2.8 调度模式选型表（用法侧规范，随形状入卷）

| 维度 | 自建 `@Scheduled` | 平台化调度（XXL-Job / ElasticJob / PowerJob） |
|------|------------------|---------------------------------------------|
| 多实例重复执行 | 每实例独立触发，需分布式锁（ShedLock 等，业务自理） | 天然规避（调度中心统一触发） |
| 失败重试 / 告警 | 自行实现 | 平台内置 |
| 动态调整 cron | 改配置重启 | 调度台在线调整 |
| 额外组件 | 无 | 调度中心 + 执行器依赖 |
| 适用场景 | 单实例 / 少量任务 | 多实例 / 任务多 / 需要运维面板 |

> 幂等责任提示（SC-8）：无论哪种模式，平台的失败重试都**不是**可重入豁免——Handler/聚合侧的
> 状态机守卫与幂等语义照常承担。

### 2.9 下游协调注记（SC-7 形状说明）

Handler 内的聚合行为方法（如 `{agg}.deliver()`）只做状态变迁与校验；需要联动其他聚合时由
Handler / DomainService **同事务直调**——
**时间只是又一种触发源，下游与各入口完全一致**。

## §3 生效登记

| 条款/环节 | 状态 | 位置 |
|---|---|---|
| SC-1/2/3 | ✅ 形态约束生效（ArchUnit + 框架件在库） | 见下两行框架件登记 |
| sample 落地 | ⛔ | 真实例映射登记于本节落位清单（调度器示例件未全量落地，本卷 §2 模板为准）；多实例分布式锁义务见 SC-4 |
| `ScheduledAdapter` 标记接口 | ✅ 已实现 | `common-ddd/adapter/task/scheduler/ScheduledAdapter.java` |
| 架构守护规则（R14a/R14b） | ✅ 已实现 | common-test `DddArchitectureRules` + 双端架构测试挂载 |
| 示例实现（OrderAutoDeliverScheduler 等，真实例映射） | ⛔ 未落地 | 本卷 §2 即落地模板；多实例分布式锁义务见 SC-4（ShedLock 等选型见 §2.8） |

> 真实例映射落位清单（⛔ 标记者未落地、按本卷 §2 模板补全）：adapter `task/scheduler/OrderAutoDeliverScheduler.java`（定时触发入口，实现标记）｜application `service/OrderAppService.java`（委托 Handler）+ `handler/command/AutoDeliverExpiredHandler.java`（批量编排）｜domain `domain/order/repository/OrderRepository.java`（新增 `findShippedBefore` 具名方法）｜infrastructure `infrastructure/persistence/master/order/repository/OrderRepositoryImpl.java`（条件查询实现 + 基类批量行为）。
