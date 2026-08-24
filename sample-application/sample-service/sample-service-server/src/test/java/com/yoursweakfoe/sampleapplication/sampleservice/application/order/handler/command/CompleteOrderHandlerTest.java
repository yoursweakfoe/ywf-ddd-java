package com.yoursweakfoe.sampleapplication.sampleservice.application.order.handler.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yoursweakfoe.sampleapplication.sampleservice.application.order.assembler.OrderAssembler;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.dto.OrderDTO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command.CompleteOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.OrderStatus;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.repository.domain.OrderRepository;
import com.yoursweakfoe.sampleapplication.sampleservice.support.TestOrders;
import com.yoursweakfoe.common.exception.type.BusinessException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompleteOrderHandlerTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderAssembler orderAssembler;
    @InjectMocks private CompleteOrderHandler handler;

    private Order createDeliveredOrder() {
        Order order = TestOrders.rebuilt(OrderStatus.DELIVERED);
        order.clearDomainEvents();
        return order;
    }

    @Test
    void handle_shouldTransitionToCompleted() {
        Order order = createDeliveredOrder();
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));
        when(orderAssembler.toDTO(any(Order.class))).thenReturn(new OrderDTO());

        OrderDTO result = handler.handle(new CompleteOrderCommand(order.getId()));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        verify(orderRepository).update(order);
        assertThat(result).isNotNull();
    }

    @Test
    void handle_shouldThrowWhenNotDelivered() {
        Order order = TestOrders.rebuilt(OrderStatus.PAID);
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> handler.handle(new CompleteOrderCommand(order.getId())))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void handle_shouldThrowWhenNotFound() {
        when(orderRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new CompleteOrderCommand(UUID.randomUUID())))
                .isInstanceOf(BusinessException.class);
    }
}
