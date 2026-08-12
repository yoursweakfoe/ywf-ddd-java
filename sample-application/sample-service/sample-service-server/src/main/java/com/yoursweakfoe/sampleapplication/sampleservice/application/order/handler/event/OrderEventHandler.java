package com.yoursweakfoe.sampleapplication.sampleservice.application.order.handler.event;

import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.event.OrderCancelledEvent;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.event.OrderCompletedEvent;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.event.OrderConfirmedEvent;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.event.OrderDeliveredEvent;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.event.OrderPaidEvent;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.event.OrderPlacedEvent;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.event.OrderShippedEvent;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.model.event.StockDeductedEvent;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.model.event.StockRestoredEvent;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.repository.OrderRepository;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.shared.service.InventoryDomainService;
import com.yoursweakfoe.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 订单领域事件监听器 —— 记录事件日志并处理取消时的库存回补。
 *
 * <p>监听器选型约定：
 * <ul>
 *   <li>纯日志等无副作用监听 —— {@code @EventListener}（事务内同步，开销可忽略）
 *   <li>补偿型副作用（如取消后回补库存）—— {@code @TransactionalEventListener(AFTER_COMMIT)}
 *       + {@code REQUIRES_NEW}：主事务（取消订单）不被副作用失败阻断，
 *       副作用失败记录 ERROR 日志供人工对账
 * </ul>
 */
@Component
public class OrderEventHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderEventHandler.class);

    // region 依赖注入
    private final OrderRepository orderRepository;
    private final InventoryDomainService inventoryDomainService;

    public OrderEventHandler(OrderRepository orderRepository,
                              InventoryDomainService inventoryDomainService) {
        this.orderRepository = orderRepository;
        this.inventoryDomainService = inventoryDomainService;
    }
    // endregion

    @EventListener
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.info("Order placed: orderId={}, totalAmount={}, customerId={}",
                event.getOrderId(), event.getTotalAmount(), event.getCustomerId());
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

    @EventListener
    public void onStockDeducted(StockDeductedEvent event) {
        log.info("Stock deducted: productId={}, quantity={}",
                event.getProductId(), event.getQuantity());
    }

    @EventListener
    public void onStockRestored(StockRestoredEvent event) {
        log.info("Stock restored: productId={}, quantity={}",
                event.getProductId(), event.getRestoredQuantity());
    }
}
