# 定时任务（Scheduler）

> 设计原理 → [module-design/adapter.md](../module-design/adapter.md)

## 业务场景

延续示例应用的电商场景（参见 [write-path.md](write-path.md) 业务场景节）。

本文以 **"自动完成超时未确认订单"** 为案例，展示 adapter 层 Scheduler 入口的完整模板。

**业务需求：**

1. 每天凌晨 2 点扫描所有 SHIPPED 状态超过 15 天的订单
2. 自动将其标记为 DELIVERED（视为买家已签收）
3. 定时任务是 adapter 层的另一种入口（与 web 并列），透传 AppService

## 调用链路

```
Spring @Scheduled 触发
  → adapter/order/scheduler/OrderAutoDeliverScheduler
    → application/order/service/OrderAppService（或直接 Handler）
      → application/order/handler/AutoDeliverExpiredOrdersHandler
        → repository 条件查询 → 逐个 order.deliver() → updateDomainBatch
```

## 1. Adapter — Scheduler 入口

```java
// adapter/order/scheduler/OrderAutoDeliverScheduler.java
package com.yoursweakfoe.sampleapplication.sampleservice.adapter.order.scheduler;

import com.yoursweakfoe.sampleapplication.sampleservice.application.order.service.OrderAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单自动签收定时任务 —— 每天凌晨 2 点执行。
 */
@Component
public class OrderAutoDeliverScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderAutoDeliverScheduler.class);

    // region 依赖注入
    private final OrderAppService orderAppService;

    public OrderAutoDeliverScheduler(OrderAppService orderAppService) {
        this.orderAppService = orderAppService;
    }
    // endregion

    /**
     * 每天凌晨 2:00 执行（cron 表达式：秒 分 时 日 月 周）。
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void autoDeliverExpiredOrders() {
        log.info("Starting auto-deliver for expired shipped orders");
        int count = orderAppService.autoDeliverExpiredOrders();
        log.info("Auto-deliver completed: {} orders processed", count);
    }
}
```

要点：
- 位于 `adapter/{agg}/scheduler/`
- `@Component` + `@Scheduled`（Spring 原生，无需额外依赖）
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

    // region 依赖注入
    private final OrderRepository orderRepository;

    public AutoDeliverExpiredOrdersHandler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
    // endregion

    @Transactional(rollbackFor = Exception.class)
    public int handle() {
        OffsetDateTime threshold = OffsetDateTime.now().minusDays(15);
        List<Order> expiredOrders = orderRepository.findShippedBefore(threshold);

        expiredOrders.forEach(Order::deliver);
        orderRepository.updateDomainBatch(expiredOrders);

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

## 分布式环境注意

| 部署模式 | 策略 |
|---------|------|
| 单实例 | 直接使用 `@Scheduled`，无需额外处理 |
| 多实例 | 需分布式锁（如 ShedLock / Redis SETNX），防止重复执行 |
| 复杂调度 | 考虑 XXL-Job / ElasticJob 等专业调度平台 |

> 当前框架不内置分布式锁，多实例部署时由业务服务自行引入 ShedLock 等组件。

## 完整文件清单

| 层 | 文件 | 职责 |
|----|------|------|
| adapter | `scheduler/OrderAutoDeliverScheduler.java` | 定时触发入口 |
| application | `OrderAppService.java` | 委托 Handler |
| application | `handler/AutoDeliverExpiredOrdersHandler.java` | 批量编排 |
| domain | `repository/OrderRepository.java` | 新增 `findShippedBefore` 方法 |
| infrastructure | `repository/OrderRepositoryImpl.java` | 实现条件查询 |
