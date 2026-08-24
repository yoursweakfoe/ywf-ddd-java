package com.yoursweakfoe.sampleapplication.sampleservice.application.order.handler.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yoursweakfoe.sampleapplication.sampleservice.application.order.assembler.OrderAssembler;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.dto.OrderDTO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command.PlaceOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.OrderStatus;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.repository.domain.OrderRepository;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.model.Product;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.repository.domain.ProductRepository;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.shared.service.InventoryDomainService;
import com.yoursweakfoe.common.exception.type.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlaceOrderHandlerTest {

    @Mock private ProductRepository productRepository;
    @Mock private InventoryDomainService inventoryDomainService;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderAssembler orderAssembler;
    @InjectMocks private PlaceOrderHandler handler;

    private PlaceOrderCommand command(Long productId, int quantity) {
        PlaceOrderCommand command = new PlaceOrderCommand();
        command.setCustomerId("customer-1");
        PlaceOrderCommand.OrderItemView itemDto = new PlaceOrderCommand.OrderItemView();
        itemDto.setProductId(productId);
        itemDto.setQuantity(quantity);
        command.setItems(List.of(itemDto));
        return command;
    }

    @Test
    void handle_shouldCreatePendingOrderWithRealUnitPrice() {
        // 商品单价 25.50，订单项小计应来自商品真实价格而非硬编码
        Product product = new Product(1L, "Widget", new BigDecimal("25.50"), 100);
        when(productRepository.findAllById(List.of(1L))).thenReturn(List.of(product));
        when(orderAssembler.toDTO(any(Order.class))).thenReturn(new OrderDTO());

        OrderDTO result = handler.handle(command(1L, 2));

        verify(inventoryDomainService).deductStock(any());
        verify(orderRepository).save(argThat((Order order) ->
                order.getStatus() == OrderStatus.PENDING
                        && order.getItems().get(0).unitPrice()
                                .compareTo(new BigDecimal("25.50")) == 0));
        assertThat(result).isNotNull();
    }

    @Test
    void handle_shouldThrowWhenProductNotFound() {
        when(productRepository.findAllById(List.of(99L))).thenReturn(List.of());

        assertThatThrownBy(() -> handler.handle(command(99L, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("product:err.notFound");
    }
}
