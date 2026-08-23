package com.yoursweakfoe.sampleapplication.sampleservice.application.order.event.listener;

import com.yoursweakfoe.sampleapplication.sampleservice.application.order.event.publisher.OrderEventPublisher;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.event.OrderCancelledEvent;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.event.OrderCompletedEvent;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.event.OrderConfirmedEvent;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.event.OrderDeliveredEvent;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.event.OrderPaidEvent;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.event.OrderPlacedEvent;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.event.OrderShippedEvent;
import com.yoursweakfoe.common.ddd.application.event.DomainEventListener;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.repository.OrderRepository;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.shared.service.InventoryDomainService;
import com.yoursweakfoe.common.exception.type.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 订单领域事件监听器（域内反应）—— 对订单聚合的领域事件作出进程内反应。
 *
 * <p>薄编排契约：方法体只做「接事件 → 加载聚合 → 委托 DomainService / Publisher」，
 * 业务规则在 DomainService 与聚合根内，本类不含 if-else 业务判断。
 *
 * <p>监听器选型约定：
 * <ul>
 *   <li>纯日志等无副作用监听 —— {@code @EventListener}（事务内同步，开销可忽略）
 *   <li>出站通知 —— {@code @TransactionalEventListener(AFTER_COMMIT)}（尽力而为，不阻断主事务）
 *   <li>补偿型副作用（如取消后回补库存）—— {@code @TransactionalEventListener(AFTER_COMMIT)}
 *       + {@code REQUIRES_NEW}：主事务（取消订单）不被副作用失败阻断，副作用失败记录 ERROR 日志供人工对账
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
     * 下单后出站通知（尽力而为）：事务提交后翻译为集成事件并投递 MQ。
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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
     * <p>{@code AFTER_COMMIT}：取消事务提交后才执行，回补失败不会回滚已取消的订单；
     * {@code REQUIRES_NEW}：原事务已完成，库存写入必须开启新事务才能提交；
     * 回补失败仅记录 ERROR 日志（人工对账），不影响已提交的取消结果。
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("Order cancelled: orderId={}, reason={}",
                event.getOrderId(), event.getReason());
        // 库存回补：根据订单明细回补库存
        try {
            Order order = orderRepository.findById(event.getOrderId())
                    .orElseThrow(() -> new BusinessException("order:err.notFound"));
            inventoryDomainService.replenishStock(order.getItems());
        } catch (Exception e) {
            // 补偿失败不向上抛（主事务已提交），记录日志供人工对账
            log.error("Stock replenish failed after order cancelled: orderId={}",
                    event.getOrderId(), e);
        }
    }
}
