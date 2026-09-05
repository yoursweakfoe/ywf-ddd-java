package com.yoursweakfoe.sampleapplication.sampleservice.application.order.handler.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yoursweakfoe.sampleapplication.sampleservice.application.order.assembler.OrderAssembler;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.dto.OrderDTO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command.PlaceOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.OrderFactory;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.OrderStatus;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.repository.OrderRepository;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.model.Product;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.repository.ProductRepository;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.shared.service.InventoryDomainService;
import com.yoursweakfoe.common.exception.type.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlaceOrderHandlerTest {

    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID_2 = UUID.randomUUID();
    private static final UUID MISSING_PRODUCT_ID = UUID.randomUUID();

    @Mock private ProductRepository productRepository;
    @Mock private InventoryDomainService inventoryDomainService;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderAssembler orderAssembler;
    /** 真实工厂实例（@Spy）：创建逻辑无外部依赖，直接复用生产实现 */
    @Spy
    private OrderFactory orderFactory = new OrderFactory();
    @InjectMocks private PlaceOrderHandler handler;

    private PlaceOrderCommand command(UUID productId, int quantity) {
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
        Product product = Product.reconstitute(PRODUCT_ID, "Widget", new BigDecimal("25.50"), 100, null, null, 0);
        when(productRepository.findAllById(List.of(PRODUCT_ID))).thenReturn(List.of(product));
        when(orderAssembler.toDTO(any(Order.class))).thenReturn(new OrderDTO());

        OrderDTO result = handler.handle(command(PRODUCT_ID, 2));

        verify(inventoryDomainService).deductStock(any());
        verify(orderRepository).save(argThat((Order order) ->
                order.getStatus() == OrderStatus.PENDING
                        && order.getItems().get(0).unitPrice()
                                .compareTo(new BigDecimal("25.50")) == 0));
        assertThat(result).isNotNull();
    }

    @Test
    void handle_shouldThrowWhenProductNotFound() {
        when(productRepository.findAllById(List.of(MISSING_PRODUCT_ID))).thenReturn(List.of());

        assertThatThrownBy(() -> handler.handle(command(MISSING_PRODUCT_ID, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("product:err.notFound");
    }

    @Test
    void handle_shouldNotSaveWhenAnyStockInsufficient() {
        // 第二个商品库存不足（DomainService 在事务内抛出）—— 整单原子失败，订单不得落库
        when(productRepository.findAllById(any())).thenReturn(List.of(
                Product.reconstitute(PRODUCT_ID, "Widget", new BigDecimal("25.50"), 100, null, null, 0),
                Product.reconstitute(PRODUCT_ID_2, "Gadget", new BigDecimal("10.00"), 1, null, null, 0)));
        // deductStock 返回 void —— 打桩必须用 doThrow().when() 形式
        doThrow(new BusinessException("product:err.insufficientStock"))
                .when(inventoryDomainService).deductStock(any());

        PlaceOrderCommand command = new PlaceOrderCommand();
        command.setCustomerId("customer-1");
        command.setItems(List.of(itemView(PRODUCT_ID, 1), itemView(PRODUCT_ID_2, 999)));

        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(BusinessException.class)
                .hasMessage("product:err.insufficientStock");

        verify(orderRepository, never()).save(any());
    }

    /** 订单项命令 DTO 工厂。 */
    private PlaceOrderCommand.OrderItemView itemView(UUID productId, int quantity) {
        PlaceOrderCommand.OrderItemView itemDto = new PlaceOrderCommand.OrderItemView();
        itemDto.setProductId(productId);
        itemDto.setQuantity(quantity);
        return itemDto;
    }
}
