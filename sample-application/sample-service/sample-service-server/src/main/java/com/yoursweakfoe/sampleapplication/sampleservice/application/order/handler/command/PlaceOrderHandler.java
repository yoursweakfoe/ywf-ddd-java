package com.yoursweakfoe.sampleapplication.sampleservice.application.order.handler.command;

import com.yoursweakfoe.sampleapplication.sampleservice.application.order.assembler.OrderAssembler;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.dto.OrderDTO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command.PlaceOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.OrderItem;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.repository.domain.OrderRepository;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.model.Product;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.repository.domain.ProductRepository;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.shared.service.InventoryDomainService;
import com.yoursweakfoe.common.ddd.application.handler.command.CommandHandler;
import com.yoursweakfoe.common.exception.type.BusinessException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 下单命令处理器 —— 跨聚合协调（商品批量加载 + 库存扣减 + 订单创建）。
 *
 * <p>这是从 AppService 拆出的复杂用例样例：
 * 依赖 4 个组件、跨 2 个聚合、含事件编排，适合独立为 Handler。
 *
 * <p>商品加载为单次 IN 批量查询；单价取自商品真实 price 字段。
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
        // 1. 批量加载商品（单次 IN 查询），以真实单价构建订单项
        Map<Long, Product> products = productRepository.findAllById(productIds(command)).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        List<OrderItem> items = command.getItems().stream()
                .map(dto -> new OrderItem(dto.getProductId(), dto.getQuantity(),
                        requireProduct(products, dto.getProductId()).getPrice()))
                .toList();

        // 2. 扣减库存（跨聚合协调；DomainService 内部批量加载，同商品数量自动合并）
        inventoryDomainService.deductStock(items);

        // 3. 创建订单并下单
        Order order = new Order(UUID.randomUUID(), items, command.getCustomerId());
        order.place();
        orderRepository.save(order);
        log.info("Order placed: orderId={}, customerId={}", order.getId(), command.getCustomerId());

        // 4. 返回 DTO
        return orderAssembler.toDTO(order);
    }

    // region 内部方法

    /** 去重后的商品 ID 集合（单次 IN 查询的输入）。 */
    private List<Long> productIds(PlaceOrderCommand command) {
        return command.getItems().stream()
                .map(PlaceOrderCommand.OrderItemView::getProductId)
                .distinct()
                .toList();
    }

    /**
     * 商品存在性守卫 —— 批量加载结果中缺失即拒绝下单。
     *
     * @throws BusinessException 商品不存在时
     */
    private Product requireProduct(Map<Long, Product> products, Long productId) {
        Product product = products.get(productId);
        if (product == null) {
            throw new BusinessException("product:err.notFound");
        }
        return product;
    }
    // endregion
}
