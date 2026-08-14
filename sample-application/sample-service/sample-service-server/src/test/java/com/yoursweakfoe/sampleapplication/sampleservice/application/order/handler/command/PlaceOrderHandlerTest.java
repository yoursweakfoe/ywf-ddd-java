package com.yoursweakfoe.sampleapplication.sampleservice.application.order.handler.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yoursweakfoe.sampleapplication.sampleservice.application.order.assembler.OrderAssembler;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.dto.OrderViewDTO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command.PlaceOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.repository.OrderRepository;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.model.Product;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.repository.ProductRepository;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.shared.service.InventoryDomainService;
import com.yoursweakfoe.common.exception.type.BusinessException;
import java.util.List;
import java.util.Optional;
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

    @Test
    void handle_shouldCreatePendingOrder() {
        Product product = new Product(1L, "Widget", 100);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(orderAssembler.toDTO(any())).thenReturn(new OrderViewDTO());

        PlaceOrderCommand command = new PlaceOrderCommand();
        command.setCustomerId("customer-1");
        PlaceOrderCommand.OrderItemView itemDto = new PlaceOrderCommand.OrderItemView();
        itemDto.setProductId(1L);
        itemDto.setQuantity(2);
        command.setItems(List.of(itemDto));

        OrderViewDTO result = handler.handle(command);

        verify(inventoryDomainService).deductStock(any());
        verify(orderRepository).save(any(Order.class));
        assertThat(result).isNotNull();
    }

    @Test
    void handle_shouldThrowWhenProductNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        PlaceOrderCommand command = new PlaceOrderCommand();
        command.setCustomerId("customer-1");
        PlaceOrderCommand.OrderItemView itemDto = new PlaceOrderCommand.OrderItemView();
        itemDto.setProductId(99L);
        itemDto.setQuantity(1);
        command.setItems(List.of(itemDto));

        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(BusinessException.class)
                .hasMessage("product:err.notFound");
    }
}
