package com.yoursweakfoe.sampleapplication.sampleservice.application.order.event.listener;

import com.yoursweakfoe.sampleapplication.sampleservice.application.order.event.publisher.OrderEventPublisher;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单领域事件监听器（域内反应）—— 对订单聚合的领域事件作出进程内反应。
 *
 * <p>薄编排契约：方法体只做「接事件 → 加载聚合 → 委托 DomainService / Publisher」，
 * 业务规则在 DomainService 与聚合根内，本类不含 if-else 业务判断。
 *
 * <p>监听器选型约定（投递时序：事件在业务事务提交后投递——本样例未提供
 * {@code OutboxStore}，走直发降级路径（afterCommit 进程内派发）；业务接入 Outbox 后
 * 由排空器投递。派发时无活动事务——见 common-ddd {@code DomainEventFlusher}）：
 * <ul>
 *   <li>一律使用普通 {@code @EventListener}——「提交后才执行」已由捕获+排空机制保证，
 *       不应使用 {@code @TransactionalEventListener(AFTER_COMMIT)}（无事务可挂靠，默认不执行）</li>
 *   <li>带数据库写入的副作用（如补偿回补）追加 {@code @Transactional(REQUIRES_NEW)}
 *       自带独立事务——派发时无活动事务，写入不会自动提交</li>
 *   <li>监听器抛异常不影响已提交的业务事务；业务接入 Outbox 后排空器可标记失败并重投
 *       （策略由排空器定）——补偿失败不再只靠静默吞掉</li>
 * </ul>
 */
@Slf4j
@Component
public class OrderDomainEventListener implements DomainEventListener {

    // region 依赖注入
    private final OrderRepository orderRepository;
    private final InventoryDomainService inventoryDomainService;
    private final OrderEventPublisher orderEventPublisher;

    public OrderDomainEventListener(OrderRepository orderRepository,
                                    InventoryDomainService inventoryDomainService,
                                    OrderEventPublisher orderEventPublisher) {
        this.orderRepository = orderRepository;
        this.inventoryDomainService = inventoryDomainService;
        this.orderEventPublisher = orderEventPublisher;
    }
    // endregion

    @EventListener
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.info("Order placed: orderId={}, totalAmount={}, customerId={}",
                event.getOrderId(), event.getTotalAmount(), event.getCustomerId());
    }

    /**
     * 下单后出站通知：投递已在业务事务提交后发生，直接翻译为集成事件并投递 MQ。
     */
    @EventListener
    public void onOrderPlacedOutbound(OrderPlacedEvent event) {
        orderEventPublisher.publishOrderPlaced(event);
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
     * <p>投递已在取消事务提交后发生（直发路径 / 业务接入 Outbox 后为排空器触发），
     * 回补失败不会回滚已取消的订单；
     * {@code REQUIRES_NEW}：派发时无活动事务，库存写入须自带独立事务才能提交；
     * 回补抛异常时：直发路径仅记日志；接入 Outbox 后由业务排空器重投（策略由排空器定）。
     */
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
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
