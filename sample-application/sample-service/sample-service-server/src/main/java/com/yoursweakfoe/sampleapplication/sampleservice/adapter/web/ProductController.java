package com.yoursweakfoe.sampleapplication.sampleservice.adapter.web;

import com.yoursweakfoe.sampleapplication.sampleservice.application.product.ProductAppService;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.api.ProductService;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.co.ProductCO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.dto.CreateProductCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.dto.GetProductQuery;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品 REST 端点 —— 纯透传到 {@link ProductAppService}。
 *
 * <p>HTTP 映射与文档注解声明在 {@link ProductService} 契约接口上（映射经接口继承），
 * 本 Controller 仅以 {@code @RestController} 标记协议并透传。
 */
@RestController
public class ProductController implements ProductService {

    private final ProductAppService productAppService;

    public ProductController(ProductAppService productAppService) {
        this.productAppService = productAppService;
    }

    @Override
    public ProductCO createProduct(CreateProductCommand command) {
        return productAppService.createProduct(command);
    }

    @Override
    public ProductCO getProduct(Long productId) {
        return productAppService.getProduct(new GetProductQuery(productId));
    }
}
