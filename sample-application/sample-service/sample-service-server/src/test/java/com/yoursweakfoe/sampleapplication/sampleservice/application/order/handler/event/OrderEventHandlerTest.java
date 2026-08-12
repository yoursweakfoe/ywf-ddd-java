package com.yoursweakfoe.sampleapplication.sampleservice.application.order.handler.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.OrderItem;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.event.OrderCancelledEvent;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.repository.OrderRepository;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.shared.service.InventoryDomainService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderEventHandlerTest {

    @Mock private OrderRepository orderRepository;
    @Mock private InventoryDomainService inventoryDomainService;
    @InjectMocks private OrderEventHandler handler;

    @Test
    void onOrderCancelled_shouldReplenishStock() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order(orderId, List.of(new OrderItem(1L, 2, BigDecimal.TEN)), "c1");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        handler.onOrderCancelled(new OrderCancelledEvent(orderId, "changed mind"));

        verify(inventoryDomainService).replenishStock(order.getItems());
    }

    @Test
    void onOrderCancelled_shouldNotThrowWhenReplenishFails() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order(orderId, List.of(new OrderItem(1L, 2, BigDecimal.TEN)), "c1");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        doThrow(new RuntimeException("DB down")).when(inventoryDomainService).replenishStock(any());

        // 不应抛异常（AFTER_COMMIT 语义：补偿失败仅记日志）
        handler.onOrderCancelled(new OrderCancelledEvent(orderId, "reason"));

        verify(inventoryDomainService).replenishStock(any());
    }

    @Test
    void onOrderCancelled_shouldNotThrowWhenOrderNotFound() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // 不应抛异常（catch 兜底）
        handler.onOrderCancelled(new OrderCancelledEvent(orderId, "reason"));

        verify(inventoryDomainService, never()).replenishStock(any());
    }
}
