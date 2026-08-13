package com.yoursweakfoe.sampleapplication.sampleservice.application.order.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yoursweakfoe.sampleapplication.sampleservice.application.order.assembler.OrderAssembler;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.dto.OrderDTO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command.ConfirmOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.OrderItem;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.OrderStatus;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.repository.OrderRepository;
import com.yoursweakfoe.common.exception.BusinessException;
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
class ConfirmOrderHandlerTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderAssembler orderAssembler;
    @InjectMocks private ConfirmOrderHandler handler;

    private static final OrderItem ITEM = new OrderItem(1L, 2, BigDecimal.TEN);

    private Order createPaidOrder() {
        Order order = new Order(UUID.randomUUID(), List.of(ITEM), "customer-1");
        order.pay();
        order.clearDomainEvents();
        return order;
    }

    @Test
    void handle_shouldTransitionToConfirmed() {
        Order order = createPaidOrder();
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));
        when(orderAssembler.toDTO(any())).thenReturn(new OrderDTO());

        OrderDTO result = handler.handle(new ConfirmOrderCommand(order.getId()));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepository).update(order);
        assertThat(result).isNotNull();
    }

    @Test
    void handle_shouldThrowWhenNotPaid() {
        Order order = new Order(UUID.randomUUID(), List.of(ITEM), "customer-1");
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> handler.handle(new ConfirmOrderCommand(order.getId())))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void handle_shouldThrowWhenNotFound() {
        when(orderRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new ConfirmOrderCommand(UUID.randomUUID())))
                .isInstanceOf(BusinessException.class);
    }
}
