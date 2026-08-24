package com.yoursweakfoe.sampleapplication.sampleservice.domain.shared.service;

import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.OrderItem;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.model.Product;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.repository.domain.ProductRepository;
import com.yoursweakfoe.common.ddd.domain.service.DomainService;
import com.yoursweakfoe.common.exception.type.BusinessException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 库存领域服务 —— 协调跨聚合的库存扣减与回补。
 *
 * <p>下单时需要同时操作多个 Product 聚合的库存，
 * 这一跨聚合协调逻辑不归属于任何单一聚合，因此封装为领域服务。
 *
 * <p>性能契约：商品按 ID 集合批量加载（单次 IN 查询），不存在逐项查询的 N+1 问题；
 * 同一商品出现在多个订单项时数量自动合并为一次聚合行为调用 + 一次持久化
 * （避免对同一聚合连续两次乐观锁 UPDATE 导致版本号踩空）。
 *
 * <p>标注 {@code @Service} 由 Spring 组件扫描自动注册（stereotype 注解为纯元数据，
 * 见 ApplicationArchitectureTest A2 白名单）。
 */
@Service
public class InventoryDomainService implements DomainService {

    // region 依赖注入
    private final ProductRepository productRepository;

    public InventoryDomainService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
    // endregion

    /**
     * 批量扣减库存（下单时调用）。
     *
     * @param items 订单项列表（同商品的数量会合并）
     * @throws BusinessException 商品不存在或库存不足时
     */
    public void deductStock(List<OrderItem> items) {
        Map<Long, Product> products = loadProducts(items);
        quantitiesByProduct(items).forEach((productId, totalQuantity) -> {
            Product product = requireProduct(products, productId);
            product.deductStock(totalQuantity);
            productRepository.update(product);
        });
    }

    /**
     * 批量回补库存（取消订单时调用）。
     *
     * @param items 订单项列表（同商品的数量会合并）
     * @throws BusinessException 商品不存在时
     */
    public void replenishStock(List<OrderItem> items) {
        Map<Long, Product> products = loadProducts(items);
        quantitiesByProduct(items).forEach((productId, totalQuantity) -> {
            Product product = requireProduct(products, productId);
            product.restoreStock(totalQuantity);
            productRepository.update(product);
        });
    }

    // region 内部方法

    /** 按 ID 集合批量加载商品（单次 IN 查询）。 */
    private Map<Long, Product> loadProducts(List<OrderItem> items) {
        List<Long> ids = items.stream().map(OrderItem::productId).distinct().toList();
        return productRepository.findAllById(ids).stream()
                .collect(LinkedHashMap::new, (map, p) -> map.put(p.getId(), p), Map::putAll);
    }

    /** 同商品订单项数量合并（保持首次出现的顺序，稳定更新次序）。 */
    private Map<Long, Integer> quantitiesByProduct(List<OrderItem> items) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (OrderItem item : items) {
            quantities.merge(item.productId(), item.quantity(), Integer::sum);
        }
        return quantities;
    }

    /**
     * 商品存在性守卫。
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
