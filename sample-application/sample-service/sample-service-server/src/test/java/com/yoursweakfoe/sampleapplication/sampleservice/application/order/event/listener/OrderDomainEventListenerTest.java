package com.yoursweakfoe.sampleapplication.sampleservice.application.order.event.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yoursweakfoe.sampleapplication.sampleservice.application.order.event.publisher.OrderEventPublisher;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.OrderStatus;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.event.domain.OrderCancelledEvent;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.event.domain.OrderPlacedEvent;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.repository.domain.OrderRepository;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.shared.service.InventoryDomainService;
import com.yoursweakfoe.sampleapplication.sampleservice.support.TestOrders;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderDomainEventListenerTest {

    @Mock private OrderRepository orderRepository;
    @Mock private InventoryDomainService inventoryDomainService;
    @Mock private OrderEventPublisher orderEventPublisher;
    @InjectMocks private OrderDomainEventListener listener;

    @Test
    void onOrderCancelled_shouldReplenishStock() {
        UUID orderId = UUID.randomUUID();
        Order order = TestOrders.rebuilt(orderId, OrderStatus.PENDING);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        listener.onOrderCancelled(new OrderCancelledEvent(orderId, "changed mind"));

        verify(inventoryDomainService).replenishStock(order.getItems());
    }

    @Test
    void onOrderCancelled_replenishFailure_propagates() {
        UUID orderId = UUID.randomUUID();
        Order order = TestOrders.rebuilt(orderId, OrderStatus.PENDING);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        doThrow(new RuntimeException("DB down")).when(inventoryDomainService).replenishStock(any());

        // 补偿失败向上抛（普通 @Transactional 加入排空器事务，随之整体回滚）：
        // 排空器据此记失败簿记并退避重投；不再静默吞掉
        // （历史行为「仅记日志」会让回补永久丢失且无任何重试线索）
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> listener.onOrderCancelled(new OrderCancelledEvent(orderId, "reason")))
                .isInstanceOf(RuntimeException.class);

        verify(inventoryDomainService).replenishStock(any());
    }

    @Test
    void onOrderCancelled_shouldNotThrowWhenOrderNotFound() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // 不应抛异常（catch 兜底）
        listener.onOrderCancelled(new OrderCancelledEvent(orderId, "reason"));

        verify(inventoryDomainService, never()).replenishStock(any());
    }

    /** 出站反应：下单事件翻译为集成事件并经集成 Outbox 捕获（委托 Publisher，不直发 MQ）。 */
    @Test
    void onOrderPlacedOutbound_delegatesToPublisher() {
        OrderPlacedEvent event = new OrderPlacedEvent(UUID.randomUUID(), BigDecimal.TEN, "customer-1");

        listener.onOrderPlacedOutbound(event);

        verify(orderEventPublisher).publishOrderPlaced(event);
    }
}
