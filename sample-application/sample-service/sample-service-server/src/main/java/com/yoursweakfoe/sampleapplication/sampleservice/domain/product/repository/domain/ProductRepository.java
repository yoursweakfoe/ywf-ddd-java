package com.yoursweakfoe.sampleapplication.sampleservice.domain.product.repository.domain;

import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.model.Product;
import com.yoursweakfoe.common.ddd.domain.repository.domain.Repository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 商品仓储接口（领域层契约）。
 */
public interface ProductRepository extends Repository<Product, Long> {

    /** 根据名称查找商品 */
    Optional<Product> findByName(String name);

    /**
     * 按 ID 集合批量加载（单次 IN 查询）。
     *
     * <p>供跨聚合协调（如库存扣减）与下单取价使用，避免逐 ID 查询的 N+1 问题。
     * 返回顺序不保证与传入顺序一致；不存在的 ID 静默缺席，由调用方做存在性守卫。
     */
    List<Product> findAllById(Collection<Long> ids);
}
