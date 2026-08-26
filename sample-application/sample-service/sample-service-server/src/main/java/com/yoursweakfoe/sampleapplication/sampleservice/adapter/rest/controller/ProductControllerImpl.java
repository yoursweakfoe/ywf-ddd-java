package com.yoursweakfoe.sampleapplication.sampleservice.adapter.rest.controller;

import com.yoursweakfoe.common.ddd.adapter.rest.controller.RestAdapter;
import com.yoursweakfoe.sampleapplication.sampleservice.application.product.service.ProductAppService;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.dto.co.ProductCO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.dto.command.CreateProductCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.dto.query.GetProductQuery;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.adapter.rest.ProductController;
import com.yoursweakfoe.common.contract.dto.query.PageResult;
import java.util.UUID;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品 REST 端点 —— 纯透传到 {@link ProductAppService}。
 *
 * <p>HTTP 映射与文档注解声明在 {@link ProductController} 契约接口上（映射经接口继承），
 * 本实现类仅以 {@code @RestController} 标记协议并透传。
 */
@RestController
public class ProductControllerImpl implements ProductController, RestAdapter {

    private final ProductAppService productAppService;

    public ProductControllerImpl(ProductAppService productAppService) {
        this.productAppService = productAppService;
    }

    @Override
    public ProductCO createProduct(CreateProductCommand command) {
        return productAppService.createProduct(command);
    }

    @Override
    public ProductCO getProduct(UUID productId) {
        return productAppService.getProduct(new GetProductQuery(productId));
    }
}
