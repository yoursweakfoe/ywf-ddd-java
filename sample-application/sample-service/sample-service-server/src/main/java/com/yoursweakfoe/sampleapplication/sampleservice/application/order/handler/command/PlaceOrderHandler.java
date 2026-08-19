package com.yoursweakfoe.sampleapplication.sampleservice.application.order.handler.command;

import com.yoursweakfoe.sampleapplication.sampleservice.application.order.assembler.OrderAssembler;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.dto.OrderDTO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command.PlaceOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.OrderItem;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.repository.OrderRepository;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.model.Product;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.repository.ProductRepository;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.shared.service.InventoryDomainService;
import com.yoursweakfoe.common.ddd.application.handler.CommandHandler;
import com.yoursweakfoe.common.exception.type.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 下单命令处理器 —— 跨聚合协调（商品查询 + 库存扣减 + 订单创建）。
 *
 * <p>这是从 AppService 拆出的复杂用例样例：
 * 依赖 5 个组件、跨 2 个聚合、含事件编排，适合独立为 Handler。
 */
@Slf4j
@Component
public class PlaceOrderHandler implements CommandHandler<PlaceOrderCommand, OrderDTO> {

    // region 依赖注入
    private final ProductRepository productRepository;
    private final InventoryDomainService inventoryDomainService;
    private final OrderRepository orderRepository;
    private final OrderAssembler orderAssembler;

    public PlaceOrderHandler(ProductRepository productRepository,
                             InventoryDomainService inventoryDomainService,
                             OrderRepository orderRepository,
                             OrderAssembler orderAssembler) {
        this.productRepository = productRepository;
        this.inventoryDomainService = inventoryDomainService;
        this.orderRepository = orderRepository;
        this.orderAssembler = orderAssembler;
    }
    // endregion

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDTO handle(PlaceOrderCommand command) {
        // 1. 构建订单项（查询商品单价）
        List<OrderItem> items = command.getItems().stream()
                .map(dto -> {
                    Product product = productRepository.findById(dto.getProductId())
                            .orElseThrow(() -> new BusinessException("product:err.notFound"));
                    // 简化：单价 = 10.00（实际系统从商品价格字段获取）
                    BigDecimal unitPrice = BigDecimal.TEN;
                    return new OrderItem(dto.getProductId(), dto.getQuantity(), unitPrice);
                })
                .toList();

        // 2. 扣减库存（跨聚合协调）
        inventoryDomainService.deductStock(items);

        // 3. 创建订单并下单
        Order order = new Order(UUID.randomUUID(), items, command.getCustomerId());
        order.place();
        orderRepository.save(order);
        log.info("Order placed: orderId={}, customerId={}", order.getId(), command.getCustomerId());

        // 4. 返回 DTO
        return orderAssembler.toDTO(order);
    }
}
