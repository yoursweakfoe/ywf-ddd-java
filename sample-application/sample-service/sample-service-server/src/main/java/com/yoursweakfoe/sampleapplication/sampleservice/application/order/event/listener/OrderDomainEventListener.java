package com.yoursweakfoe.sampleapplication.sampleservice.application.order.event.listener;

import com.yoursweakfoe.sampleapplication.sampleservice.application.order.event.capture.OrderIntegrationEventCapture;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.event.domain.OrderCancelledEvent;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.event.domain.OrderCompletedEvent;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.event.domain.OrderConfirmedEvent;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.event.domain.OrderDeliveredEvent;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.event.domain.OrderPaidEvent;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.event.domain.OrderPlacedEvent;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.event.domain.OrderShippedEvent;
import com.yoursweakfoe.common.ddd.application.event.listener.DomainEventListener;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.repository.domain.OrderRepository;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.shared.service.InventoryDomainService;
import com.yoursweakfoe.common.exception.type.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单领域事件监听器（域内反应）—— 对订单聚合的领域事件作出进程内反应。
 *
 * <p>薄编排契约：方法体只做「接事件 → 加载聚合 → 委托 DomainService / Capture」，
 * 业务规则在 DomainService 与聚合根内，本类不含 if-else 业务判断。
 *
 * <p>监听器选型约定（全链路 Outbox 规范：投递发生在框架排空器 {@code OutboxRelay} 领域实例
 * 的自有事务内——排空器认领 {@code ddd_domain_event_outbox} 行后于该事务中派发，
 * 监听器副作用 / 集成入箱 / 领域行标记完成三者原子提交）：
 * <ul>
 *   <li>一律使用普通 {@code @EventListener}；带数据库写入的副作用用普通 {@code @Transactional}
 *       （<strong>加入</strong>排空器事务）——<strong>禁用</strong> {@code REQUIRES_NEW} 与 {@code @Async}，
 *       二者都会撕碎「副作用 + 集成入箱 + 标记完成」的原子性，重试时产生双份副作用</li>
 *   <li>监听器不做任何非事务副作用（HTTP / 直发 MQ）——对外通知一律经集成 Outbox 捕获
 *       （见 {@link #onOrderPlacedOutbound}）</li>
 *   <li>监听器抛异常向上传播 → 排空器事务回滚 → 行保持待投 → 退避重投（策略由排空器定），
 *       消费端以 {@code eventId} 幂等去重（at-least-once）</li>
 * </ul>
 */
@Slf4j
@Component
public class OrderDomainEventListener implements DomainEventListener {

    // region 依赖注入
    private final OrderRepository orderRepository;
    private final InventoryDomainService inventoryDomainService;
    private final OrderIntegrationEventCapture orderIntegrationEventCapture;

    public OrderDomainEventListener(OrderRepository orderRepository,
                                    InventoryDomainService inventoryDomainService,
                                    OrderIntegrationEventCapture orderIntegrationEventCapture) {
        this.orderRepository = orderRepository;
        this.inventoryDomainService = inventoryDomainService;
        this.orderIntegrationEventCapture = orderIntegrationEventCapture;
    }
    // endregion

    @EventListener
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.info("Order placed: orderId={}, totalAmount={}, customerId={}",
                event.getOrderId(), event.getTotalAmount(), event.getCustomerId());
    }

    /**
     * 下单后出站通知：在排空器事务内把领域事件翻译为集成事件并经集成 Outbox 捕获
     * （与「领域行标记完成」原子入箱），实际投 MQ 由框架集成排空器完成——关闭 dual-write 窗口。
     */
    @EventListener
    public void onOrderPlacedOutbound(OrderPlacedEvent event) {
        orderIntegrationEventCapture.publishOrderPlaced(event);
    }

    @EventListener
    public void onOrderPaid(OrderPaidEvent event) {
        log.info("Order paid: orderId={}", event.getOrderId());
    }

    @EventListener
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        log.info("Order confirmed: orderId={}", event.getOrderId());
    }

    @EventListener
    public void onOrderShipped(OrderShippedEvent event) {
        log.info("Order shipped: orderId={}, trackingNumber={}",
                event.getOrderId(), event.getTrackingNumber());
    }

    @EventListener
    public void onOrderDelivered(OrderDeliveredEvent event) {
        log.info("Order delivered: orderId={}", event.getOrderId());
    }

    @EventListener
    public void onOrderCompleted(OrderCompletedEvent event) {
        log.info("Order completed: orderId={}", event.getOrderId());
    }

    /**
     * 订单取消后回补库存（补偿型副作用）。
     *
     * <p>投递发生在排空器事务内，本方法以普通 {@code @Transactional}（REQUIRED）
     * <strong>加入</strong>该事务——回补写入与「（可能的集成入箱）+ 领域行标记完成」原子提交；
     * 回补失败 → 排空器事务回滚 → 行保持待投 → 退避重投，不再静默吞掉。
     * <strong>禁用</strong> {@code REQUIRES_NEW}（会撕碎原子性，重试时产生双份回补）。
     */
    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("Order cancelled: orderId={}, reason={}",
                event.getOrderId(), event.getReason());
        // 库存回补：根据订单明细回补库存
        Order order;
        try {
            order = orderRepository.findById(event.getOrderId())
                    .orElseThrow(() -> new BusinessException("order:err.notFound"));
        } catch (Exception e) {
            // 订单不可读（极罕见）：无法取得明细，记录告警供人工核对
            log.error("Stock replenish aborted: order unreadable after cancel: orderId={}",
                    event.getOrderId(), e);
            return;
        }
        inventoryDomainService.replenishStock(order.getItems());
    }
}
