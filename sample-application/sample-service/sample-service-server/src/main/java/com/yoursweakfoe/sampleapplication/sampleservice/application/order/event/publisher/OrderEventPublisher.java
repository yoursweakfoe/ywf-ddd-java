package com.yoursweakfoe.sampleapplication.sampleservice.application.order.event.publisher;

import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.event.integration.OrderPlacedIntegrationEvent;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.event.OrderPlacedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 订单事件出站 Publisher —— 将领域事件翻译为集成事件并投递 MQ。
 *
 * <p>当前为日志占位，待 common-mq 模块建设后接入 RocketMQTemplate。
 * 翻译契约：领域事件（丰富领域细节）→ 集成事件（仅外部需要的精简字段）。
 *
 * <p>只被域内反应监听器（DomainEventListener）或 Handler 显式调用，不被 AppService 直接调用。
 */
@Slf4j
@Component
public class OrderEventPublisher {

    /**
     * 发布「订单已下单」集成事件。
     *
     * @param domainEvent 领域事件（订单已下单）
     */
    public void publishOrderPlaced(OrderPlacedEvent domainEvent) {
        OrderPlacedIntegrationEvent ie = new OrderPlacedIntegrationEvent(
                domainEvent.getOrderId().toString(), domainEvent.getCustomerId());
        log.info("Publishing integration event: {}", ie);
    }
}
