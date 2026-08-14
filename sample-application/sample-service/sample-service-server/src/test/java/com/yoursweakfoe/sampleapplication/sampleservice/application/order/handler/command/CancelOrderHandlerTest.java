package com.yoursweakfoe.sampleapplication.sampleservice.application.order.handler.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command.CancelOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.OrderItem;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.OrderStatus;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.repository.OrderRepository;
import com.yoursweakfoe.common.exception.type.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * CancelOrderHandler 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class CancelOrderHandlerTest {

    @Mock
    private OrderRepository orderRepository;
    @InjectMocks
    private CancelOrderHandler handler;

    private static final OrderItem ITEM = new OrderItem(1L, 2, BigDecimal.TEN);

    private Order createPendingOrder() {
        return new Order(UUID.randomUUID(), List.of(ITEM), "customer-1");
    }

    @Test
    void handle_shouldCancelPendingOrder() {
        // Given
        Order order = createPendingOrder();
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));

        // When
        handler.handle(new CancelOrderCommand(order.getId().toString(), "no longer needed"));

        // Then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelReason()).isEqualTo("no longer needed");
        verify(orderRepository).update(order);
    }

    @Test
    void handle_shouldCancelPaidOrder() {
        // Given
        Order order = createPendingOrder();
        order.pay();
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));

        // When
        handler.handle(new CancelOrderCommand(order.getId().toString(), "refund"));

        // Then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void handle_shouldThrowWhenNotFound() {
        // Given
        when(orderRepository.findById(any())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> handler.handle(new CancelOrderCommand(UUID.randomUUID().toString(), "x")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void handle_shouldThrowWhenAlreadyConfirmed() {
        // Given
        Order order = createPendingOrder();
        order.pay();
        order.confirm();
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> handler.handle(new CancelOrderCommand(order.getId().toString(), "too late")))
                .isInstanceOf(BusinessException.class);
    }
}
