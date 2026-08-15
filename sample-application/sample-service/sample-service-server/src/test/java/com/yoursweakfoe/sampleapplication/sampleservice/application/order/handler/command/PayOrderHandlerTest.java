package com.yoursweakfoe.sampleapplication.sampleservice.application.order.handler.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yoursweakfoe.sampleapplication.sampleservice.application.order.assembler.OrderAssembler;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.dto.OrderDTO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command.PayOrderCommand;
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
 * PayOrderHandler 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class PayOrderHandlerTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderAssembler orderAssembler;
    @InjectMocks
    private PayOrderHandler handler;

    private static final OrderItem ITEM = new OrderItem(1L, 2, BigDecimal.TEN);

    private Order createPendingOrder() {
        return new Order(UUID.randomUUID(), List.of(ITEM), "customer-1");
    }

    @Test
    void handle_shouldTransitionToPaid() {
        // Given
        Order order = createPendingOrder();
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));
        when(orderAssembler.toDTO(any(Order.class))).thenReturn(new OrderDTO());

        // When
        OrderDTO result = handler.handle(new PayOrderCommand(order.getId()));

        // Then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderRepository).update(order);
        assertThat(result).isNotNull();
    }

    @Test
    void handle_shouldThrowWhenNotFound() {
        // Given
        when(orderRepository.findById(any())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> handler.handle(new PayOrderCommand(UUID.randomUUID())))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void handle_shouldThrowWhenAlreadyPaid() {
        // Given
        Order order = createPendingOrder();
        order.pay();  // 先支付一次
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));

        // When & Then（重复支付应抛异常）
        assertThatThrownBy(() -> handler.handle(new PayOrderCommand(order.getId())))
                .isInstanceOf(BusinessException.class);
    }
}
