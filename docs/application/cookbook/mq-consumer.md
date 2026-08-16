# MQ 消费者（Consumer）

> ⛔ **本文为设计模板，尚未实现**：Consumer 依赖 common-mq 模块（RocketMQTemplate），该模块尚未建设，示例应用当前无 Consumer 实现。建设完成后按本文模板落地 `adapter/consumer/`，AppService → CommandHandler 写路径零改动复用。
>
> 设计原理 → [module-design/adapter.md](../module-design/adapter.md)

## 业务场景

延续示例应用的电商场景（参见 [write-path.md](write-path.md) 业务场景节）。

本文以 **"消费支付成功消息 → 更新订单状态"** 为案例，展示 adapter 层 MQ Consumer 入口的完整模板。

**业务需求：**

1. 支付服务完成扣款后，向 MQ 投递 `PaymentSucceededIntegrationEvent`
2. 订单服务消费该消息，将对应订单标记为 PAID
3. Consumer 是 adapter 层的另一种入口（与 Facade、Scheduler 并列），透传 AppService

## 调用链路

```
MQ Broker（RocketMQ / Kafka）
  → adapter/consumer/PaymentEventConsumer
    → 反序列化 → 构建 Command
    → application/order/OrderAppService.payOrder(command)
      → PayOrderHandler（复用已有写路径）
```

## 1. Adapter — Consumer 入口

```java
// adapter/consumer/PaymentEventConsumer.java
package com.yoursweakfoe.sampleapplication.sampleservice.adapter.consumer;

import com.yoursweakfoe.sampleapplication.sampleservice.application.order.OrderAppService;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command.PayOrderCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 支付事件消费者 —— 接收支付成功消息，触发订单状态变迁。
 *
 * <p>当前为模板代码，MQ 监听注解待 common-mq 模块建设后接入。
 */
@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    // region 依赖注入
    private final OrderAppService orderAppService;

    public PaymentEventConsumer(OrderAppService orderAppService) {
        this.orderAppService = orderAppService;
    }
    // endregion

    /**
     * 消费支付成功事件。
     *
     * <p>实际接入时添加 MQ 监听注解（如 RocketMQ 的 @RocketMQMessageListener）。
     */
    // @RocketMQMessageListener(topic = "payment-topic", consumerGroup = "order-service")
    public void onPaymentSucceeded(String messageBody) {
        log.info("Received payment succeeded event: {}", messageBody);

        // 1. 反序列化（实际使用 JSON 解析）
        // PaymentSucceededIntegrationEvent event = JSON.parseObject(messageBody, ...);
        String orderId = extractOrderId(messageBody);

        // 2. 构建 Command → 透传 AppService（复用已有写路径）
        orderAppService.payOrder(new PayOrderCommand(java.util.UUID.fromString(orderId)));

        log.info("Order paid via MQ event: orderId={}", orderId);
    }

    private String extractOrderId(String messageBody) {
        // 简化：实际使用 JSON 解析
        return messageBody;
    }
}
```

要点：
- 位于 `adapter/consumer/`
- `@Component`（MQ 监听注解待 common-mq 模块建设后补充）
- **纯透传** AppService，不含业务逻辑
- 复用已有 Handler（`PayOrderHandler`），不重复编写业务代码

## 2. 幂等性保障

MQ 消息可能重复投递（网络抖动、Broker 重试），Consumer 必须保证幂等：

| 策略 | 实现方式 | 适用场景 |
|------|---------|---------|
| 状态机天然幂等 | `order.pay()` 内 requireState(PENDING) → 重复消费时抛异常 → 忽略 | 状态变迁类 |
| 去重表 | 消费前 INSERT messageId（唯一键），冲突则跳过 | 非幂等操作（如扣款） |
| 乐观锁 | version 字段保护，重复消费时 UPDATE 影响 0 行 → 忽略 | 通用 |

### 状态机幂等示例

```java
public void onPaymentSucceeded(String messageBody) {
    try {
        orderAppService.payOrder(new PayOrderCommand(orderId));
    } catch (BusinessException e) {
        // 订单已非 PENDING 状态 → 重复消费，安全忽略
        if ("order:err.status.pending".equals(e.getMessageKey())) {
            log.info("Duplicate message ignored: orderId={}", orderId);
            return;
        }
        throw e;  // 其他业务异常继续传播
    }
}
```

## 3. 死信处理

| 重试次数 | 处理方式 |
|:--------:|---------|
| 1-3 次 | MQ 自动重试（Broker 配置） |
| 超过阈值 | 进入死信队列（DLQ），人工介入 |
| 人工处理 | 管理后台查看死信消息，修复后手动重投 |

> 死信队列配置属于 MQ 基础设施（Broker 侧），不在应用代码中处理。

## 完整文件清单

| 层 | 文件 | 职责 |
|----|------|------|
| adapter | `consumer/PaymentEventConsumer.java` | MQ 消费入口 |
| application | `OrderAppService.java` | 委托 Handler（复用） |
| application | `handler/PayOrderHandler.java` | 已有写路径（复用） |
| contract | `dto/command/PayOrderCommand.java` | 命令对象（复用） |
