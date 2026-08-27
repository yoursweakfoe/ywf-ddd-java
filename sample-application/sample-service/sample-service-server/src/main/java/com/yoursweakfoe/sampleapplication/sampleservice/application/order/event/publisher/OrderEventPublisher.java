package com.yoursweakfoe.sampleapplication.sampleservice.application.order.event.publisher;

import com.yoursweakfoe.common.ddd.application.event.outbox.IntegrationEventOutboxStore;
import com.yoursweakfoe.common.ddd.application.event.publisher.IntegrationEventPublisher;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.event.integration.OrderPlacedIntegrationEvent;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.event.domain.OrderPlacedEvent;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 订单事件出站 Publisher —— 将领域事件翻译为集成事件并捕获入集成 Outbox。
 *
 * <p><strong>全链路 Outbox 规范（职责重定义）</strong>：本组件只做
 * <strong>翻译 + 同事务捕获</strong>，不直接投 MQ——由域内反应监听器在
 * <strong>领域排空事务内</strong>调用，翻译出的集成事件经 {@link IntegrationEventOutboxStore}
 * 与「领域行标记完成」原子入箱（{@code ddd_integration_event_outbox}），
 * 关闭「领域事件已派发 → 集成事件投 MQ」之间的 dual-write 窗口；
 * 实际投递由框架集成排空器经 {@code IntegrationEventSender} 完成（messageId = outbox 行 id）。
 *
 * <p>翻译契约：领域事件（丰富领域细节）→ 集成事件（仅外部需要的精简字段）。
 * 一个领域事件可 fan-out 为 1..N 个集成事件（此处为一对一示例）。
 *
 * <p>只被域内反应监听器（DomainEventListener）或 Handler 显式调用，不被 AppService 直接调用。
 */
@Slf4j
@Component
public class OrderEventPublisher implements IntegrationEventPublisher {

    private final IntegrationEventOutboxStore integrationEventOutboxStore;

    public OrderEventPublisher(IntegrationEventOutboxStore integrationEventOutboxStore) {
        this.integrationEventOutboxStore = integrationEventOutboxStore;
    }

    /**
     * 发布「订单已下单」集成事件 —— 翻译并捕获入集成 Outbox（调用方事务内）。
     *
     * @param domainEvent 领域事件（订单已下单），作为集成事件的溯源血缘（source_event_id）
     */
    public void publishOrderPlaced(OrderPlacedEvent domainEvent) {
        OrderPlacedIntegrationEvent ie = new OrderPlacedIntegrationEvent(
                domainEvent.getOrderId().toString(), domainEvent.getCustomerId());
        integrationEventOutboxStore.appendAll(domainEvent, List.of(ie));
        log.info("Captured integration event into outbox: {}", ie);
    }
}
