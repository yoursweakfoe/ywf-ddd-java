# 定时任务全链路

> 设计原理 → [module-design/adapter.md](../module-design/adapter.md)
> 同类入口参照 → [event-flow.md](event-flow.md)（MQ 入口）/ [write-path.md](write-path.md)（web 入口）

## 业务场景

延续示例应用的电商场景（参见 [write-path.md](write-path.md) 业务场景节）。

本文以 **"自动完成超时未确认订单"** 为案例，完整展示时间驱动入口的标记、触发、透传与批量编排。

**为什么定时任务是独立的一类入口？**

| 如果把调度逻辑写在别处 | 独立 Scheduler 入口 |
|---|---|
| 触发逻辑散落在 Controller / 事件监听器里 | 时间触发集中在 `@Scheduled` 方法，一处可见 |
| 无法被架构规则定位与约束 | `ScheduledAdapter` 标记 + ArchUnit R14a/R14b 守护 |
| 批量编排与业务规则混杂 | 纯透传 AppService，批量编排在 Handler |

## 三类入口对照

adapter 层共有三类 driving adapter（协议适配入口），Scheduler 是其中由**时间**驱动的一类：

| 入口标记 | 驱动源 | 包位置 | 架构规则 |
|---------|--------|--------|---------|
| `RestAdapter` | HTTP 请求 | `adapter/{agg}/rest/controller` | R8a / R8b |
| `IntegrationEventConsumer` | MQ 消息 | `adapter/{agg}/event/consumer` | R9a / R9b |
| **`ScheduledAdapter`** | **时间类调度（自建 @Scheduled 或 XXL-Job / Quartz 等平台化）** | **`adapter/{agg}/task/scheduler`** | **R14a / R14b** |

> 注意区分：框架的 Outbox 排空调度器（`OutboxRelayScheduler`，common-ddd
> `infrastructure/event/outbox/scheduler/`）也用 `@Scheduled`，但它是**框架管线**
> （基础设施自驱排空 `ddd_domain_event_outbox` / `ddd_integration_event_outbox`，
> 不含业务编排），**不实现** `ScheduledAdapter` 标记，不属于本文的业务定时入口
> （见 [event-flow.md](event-flow.md) 排空器节）。

## 链路全景

```
Spring @Scheduled 触发（cron 到点）
  → OrderAutoDeliverScheduler.autoDeliverExpiredOrders()   ① 时间驱动入口（纯透传）
    → OrderAppService.autoDeliverExpiredOrders()           ② 用例门面（委托 Handler）
      → AutoDeliverExpiredOrdersHandler.handle()           ③ 批量编排（@Transactional）
        → orderRepository.findShippedBefore(threshold)     ④ 条件查询（超时 SHIPPED 订单）
        → order.deliver() × N                              ⑤ 聚合行为（状态机变迁，可注册事件）
        → orderRepository.updateDomainBatch(expiredOrders) ⑥ 批量落库（逐条 validate + 事件发布）
```

## 链路图

```mermaid
graph TB
    CLOCK[时间触发<br/>@Scheduled cron] --> SCH[OrderAutoDeliverScheduler<br/>implements ScheduledAdapter]

    SCH -->|纯透传| AS[OrderAppService]
    AS --> H[AutoDeliverExpiredOrdersHandler<br/>@Transactional]

    H --> Q[findShippedBefore<br/>条件查询]
    Q --> AGG[Order.deliver × N<br/>聚合行为]
    AGG --> UPD[updateDomainBatch<br/>批量落库 + 事件发布]
    UPD --> DONE[批量处理完成<br/>返回处理条数]
```

## 实现状态

> 各环节在当前示例应用 / 框架中的落地情况；「未实现」的环节待业务需要时按本文模板补全。

| 环节 | 状态 | 落地位置 |
|------|------|---------|
| `ScheduledAdapter` 标记接口 | ✅ 已实现 | `common-ddd/adapter/task/scheduler/ScheduledAdapter.java` |
| 架构守护规则（R14a/R14b） | ✅ 已实现 | common-test `DDDArchitectureRules` + 双端架构测试挂载 |
| 示例实现（OrderAutoDeliverScheduler 等） | ⛔ 未落地 | 本文即落地模板；多实例分布式锁见文末注意事项 |

## 1. Adapter — Scheduler 入口

```java
// adapter/order/task/scheduler/OrderAutoDeliverScheduler.java
package com.yoursweakfoe.sampleapplication.sampleservice.adapter.order.task.scheduler;

import com.yoursweakfoe.common.ddd.adapter.task.scheduler.ScheduledAdapter;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.service.OrderAppService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单自动签收定时任务 —— 每天凌晨 2 点执行。
 */
@Slf4j
@Component
public class OrderAutoDeliverScheduler implements ScheduledAdapter {   // ← R14b：包下类必须实现标记

    private final OrderAppService orderAppService;

    public OrderAutoDeliverScheduler(OrderAppService orderAppService) {
        this.orderAppService = orderAppService;
    }

    /** 每天凌晨 2:00 执行（cron 表达式：秒 分 时 日 月 周）。 */
    @Scheduled(cron = "0 0 2 * * ?")
    public void autoDeliverExpiredOrders() {
        log.info("Starting auto-deliver for expired shipped orders");
        int count = orderAppService.autoDeliverExpiredOrders();
        log.info("Auto-deliver completed: {} orders processed", count);
    }
}
```

要点：
- 位于 `adapter/{agg}/task/scheduler/`，**实现 `ScheduledAdapter` 标记**（R14a：标记类必须在 adapter 层；R14b：包下类必须带标记）
- 触发注解按调度模式选择：自建 `@Scheduled`（Spring 原生，无需额外依赖）或平台化 handler 注解（如 XXL-Job 的 `@XxlJob`）——标记与规则对两者一视同仁
- **纯透传** AppService，不含业务逻辑
- 日志记录执行开始/结束（运维可观测）

## 2. Application — AppService 方法

```java
// application/order/service/OrderAppService.java（节选）
public int autoDeliverExpiredOrders() {
    return autoDeliverExpiredOrdersHandler.handle();
}
```

## 3. Application — Handler

```java
// application/order/handler/AutoDeliverExpiredOrdersHandler.java
@Component
public class AutoDeliverExpiredOrdersHandler {

    private final OrderRepository orderRepository;

    public AutoDeliverExpiredOrdersHandler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public int handle() {
        OffsetDateTime threshold = OffsetDateTime.now().minusDays(15);
        List<Order> expiredOrders = orderRepository.findShippedBefore(threshold);

        expiredOrders.forEach(order -> {           // 聚合行为：deliver() 内部可 registerEvent
            order.deliver();
        });
        orderRepository.updateDomainBatch(expiredOrders);   // 逐条 validate + 事件发布

        return expiredOrders.size();
    }
}
```

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

## 与事件链路的组合

定时任务入口与 [event-flow.md](event-flow.md) 的事件链路天然衔接：Handler 内的聚合行为方法
（如 `order.deliver()`）照常 `registerEvent`，仓储 `updateDomainBatch` 逐条「先落库后发布」，
后续的域内反应 / 集成事件出站完全复用事件链路——**时间只是第三种触发源，下游管线不变**。

## 5. 平台化调度变体（XXL-Job / ElasticJob / Quartz）

平台化调度下**仅触发注解不同**，标记、透传与下游链路完全一致：

```java
// XXL-Job 变体
@Slf4j
@Component
public class OrderAutoDeliverScheduler implements ScheduledAdapter {   // 标记不变

    @XxlJob("orderAutoDeliverHandler")                                 // 触发注解换平台
    public void autoDeliverExpiredOrders() {
        log.info("Starting auto-deliver for expired shipped orders");
        int count = orderAppService.autoDeliverExpiredOrders();
        log.info("Auto-deliver completed: {} orders processed", count);
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

| 层 | 文件 | 职责 |
|----|------|------|
| common-ddd | `adapter/task/scheduler/ScheduledAdapter.java` | 入口角色标记（✅ 框架已备） |
| adapter | `scheduler/OrderAutoDeliverScheduler.java` | 定时触发入口（实现标记，⛔ 待落地） |
| application | `OrderAppService.java` | 委托 Handler |
| application | `handler/AutoDeliverExpiredOrdersHandler.java` | 批量编排 |
| domain | `repository/OrderRepository.java` | 新增 `findShippedBefore` 方法 |
| infrastructure | `repository/OrderRepositoryImpl.java` | 实现条件查询 |
