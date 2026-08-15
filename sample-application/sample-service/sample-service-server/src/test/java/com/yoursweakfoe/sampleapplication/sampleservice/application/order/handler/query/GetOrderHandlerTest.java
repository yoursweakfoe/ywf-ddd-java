package com.yoursweakfoe.sampleapplication.sampleservice.application.order.handler.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.yoursweakfoe.sampleapplication.sampleservice.application.order.assembler.OrderAssembler;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.dto.OrderViewDTO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.query.GetOrderQuery;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.OrderReadView;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.repository.OrderRepository;
import com.yoursweakfoe.common.exception.type.BusinessException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * GetOrderHandler 单元测试 —— 读侧投影链路。
 */
@ExtendWith(MockitoExtension.class)
class GetOrderHandlerTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderAssembler orderAssembler;
    @InjectMocks
    private GetOrderHandler handler;

    @Test
    void handle_shouldProjectReadViewToDto() {
        // Given
        OrderReadView view = new OrderReadView(
                "order-id", "PENDING", List.of(), null, "cust-1", null, null, null, null);
        when(orderRepository.findReadView(any())).thenReturn(Optional.of(view));
        when(orderAssembler.toDTO(view)).thenReturn(new OrderViewDTO());

        // When
        OrderViewDTO result = handler.handle(new GetOrderQuery(UUID.randomUUID().toString()));

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    void handle_shouldThrowWhenNotFound() {
        // Given
        when(orderRepository.findReadView(any())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> handler.handle(new GetOrderQuery(UUID.randomUUID().toString())))
                .isInstanceOf(BusinessException.class)
                .hasMessage("order:err.notFound");
    }
}
