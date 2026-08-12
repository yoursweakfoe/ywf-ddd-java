package com.yoursweakfoe.sampleapplication.sampleservice.adapter.web;

import com.yoursweakfoe.sampleapplication.sampleservice.application.product.ProductAppService;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.api.ProductService;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.co.ProductCO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.dto.CreateProductCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.dto.GetProductQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品 REST 端点 —— 纯透传到 {@link ProductAppService}。
 *
 * <p>对外 REST 面以本 Controller 为准（spring-web 原生注解显式声明路径），
 * 实现 {@link ProductService} 契约接口以保持方法签名与契约一致。
 */
@RestController
@RequestMapping("/products")
@Tag(name = "商品服务", description = "商品创建与查询")
public class ProductController implements ProductService {

    // region 依赖注入
    private final ProductAppService productAppService;

    public ProductController(ProductAppService productAppService) {
        this.productAppService = productAppService;
    }
    // endregion

    @Override
    @PostMapping("")
    @Operation(summary = "创建商品", description = "新增商品并初始化库存")
    public ProductCO createProduct(@RequestBody CreateProductCommand command) {
        return productAppService.createProduct(command);
    }

    @Override
    @GetMapping("/{productId}")
    @Operation(summary = "查询商品详情", description = "根据 ID 获取商品信息")
    public ProductCO getProduct(
            @PathVariable("productId") @Parameter(description = "商品 ID") Long productId) {
        return productAppService.getProduct(new GetProductQuery(productId));
    }
}
