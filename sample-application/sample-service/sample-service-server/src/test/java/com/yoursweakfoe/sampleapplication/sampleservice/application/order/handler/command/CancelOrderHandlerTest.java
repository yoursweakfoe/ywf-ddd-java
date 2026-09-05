package com.yoursweakfoe.sampleapplication.sampleservice.application.order.handler.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command.CancelOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.OrderStatus;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.repository.domain.OrderRepository;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.shared.service.InventoryDomainService;
import com.yoursweakfoe.sampleapplication.sampleservice.support.TestOrders;
import com.yoursweakfoe.common.exception.type.BusinessException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * CancelOrderHandler 单元测试 —— 取消与库存回补同事务直调。
 */
@ExtendWith(MockitoExtension.class)
class CancelOrderHandlerTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private InventoryDomainService inventoryDomainService;
    @InjectMocks
    private CancelOrderHandler handler;

    private Order createPendingOrder() {
        return TestOrders.rebuilt(OrderStatus.PENDING);
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
        // 同事务补偿：回补以订单明细直调 DomainService
        verify(inventoryDomainService).replenishStock(order.getItems());
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
