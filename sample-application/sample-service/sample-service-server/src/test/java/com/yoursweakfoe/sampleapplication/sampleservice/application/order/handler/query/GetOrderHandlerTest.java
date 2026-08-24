package com.yoursweakfoe.sampleapplication.sampleservice.application.order.handler.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.yoursweakfoe.sampleapplication.sampleservice.application.order.dto.OrderViewDTO;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.repository.application.OrderQueryRepository;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.query.GetOrderQuery;
import com.yoursweakfoe.common.exception.type.BusinessException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * GetOrderHandler 单元测试 —— 读侧绕过 domain，PO → DTO 直接投影链路。
 */
@ExtendWith(MockitoExtension.class)
class GetOrderHandlerTest {

    @Mock
    private OrderQueryRepository orderQueryRepository;
    @InjectMocks
    private GetOrderHandler handler;

    @Test
    void handle_shouldProjectReadDto() {
        // Given
        when(orderQueryRepository.findById(any())).thenReturn(Optional.of(new OrderViewDTO()));

        // When
        OrderViewDTO result = handler.handle(new GetOrderQuery(UUID.randomUUID()));

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    void handle_shouldThrowWhenNotFound() {
        // Given
        when(orderQueryRepository.findById(any())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> handler.handle(new GetOrderQuery(UUID.randomUUID())))
                .isInstanceOf(BusinessException.class)
                .hasMessage("order:err.notFound");
    }
}
