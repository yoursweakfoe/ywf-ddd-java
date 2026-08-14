package com.yoursweakfoe.sampleapplication.sampleservice.domain.shared.service;

import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.OrderItem;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.model.Product;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.repository.ProductRepository;
import com.yoursweakfoe.common.ddd.domain.service.DomainService;
import com.yoursweakfoe.common.exception.type.BusinessException;
import java.util.List;

/**
 * 库存领域服务 —— 协调跨聚合的库存扣减与回补。
 *
 * <p>下单时需要同时操作多个 Product 聚合的库存，
 * 这一跨聚合协调逻辑不归属于任何单一聚合，因此封装为领域服务。
 *
 * <p>Bean 注册由 {@code infrastructure.config.DomainServiceConfig} 负责，
 * 保持领域层零框架依赖。
 */
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
     * @param items 订单项列表
     * @throws BusinessException 商品不存在或库存不足时
     */
    public void deductStock(List<OrderItem> items) {
        for (OrderItem item : items) {
            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new BusinessException("product:err.notFound"));
            product.deductStock(item.quantity());
            productRepository.update(product);
        }
    }

    /**
     * 批量回补库存（取消订单时调用）。
     *
     * @param items 订单项列表
     */
    public void replenishStock(List<OrderItem> items) {
        for (OrderItem item : items) {
            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new BusinessException("product:err.notFound"));
            product.restoreStock(item.quantity());
            productRepository.update(product);
        }
    }
}
