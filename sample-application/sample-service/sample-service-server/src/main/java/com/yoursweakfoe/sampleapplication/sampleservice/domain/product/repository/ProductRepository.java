package com.yoursweakfoe.sampleapplication.sampleservice.domain.product.repository;

import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.model.Product;
import com.yoursweakfoe.common.ddd.domain.repository.Repository;
import java.util.Optional;

/**
 * 商品仓储接口（领域层契约）。
 */
public interface ProductRepository extends Repository<Product, Long> {

    /** 根据名称查找商品 */
    Optional<Product> findByName(String name);
}
