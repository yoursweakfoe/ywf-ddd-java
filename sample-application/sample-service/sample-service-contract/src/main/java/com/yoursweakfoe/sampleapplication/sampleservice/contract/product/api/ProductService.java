package com.yoursweakfoe.sampleapplication.sampleservice.contract.product.api;

import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.co.ProductCO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.dto.CreateProductCommand;

/**
 * 商品服务接口 —— 服务内部用例契约（方法签名单一事实源）。
 *
 * <p>对外 REST 面由服务端 {@code adapter.web.ProductController} 以 spring-web
 * 原生注解显式声明路径并实现本接口；东西向内部查询走 proto 契约
 * {@code ProductInternalService}（src/main/proto）。
 */
public interface ProductService {

    /**
     * 创建商品。
     *
     * @param command 创建商品命令（名称 + 初始库存）
     * @return 创建后的商品信息
     */
    ProductCO createProduct(CreateProductCommand command);

    /**
     * 查询商品详情。
     *
     * @param productId 商品 ID
     * @return 商品完整信息
     */
    ProductCO getProduct(Long productId);
}
