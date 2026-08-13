package com.yoursweakfoe.sampleapplication.sampleservice.contract.product.api;

import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.co.ProductCO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.dto.CreateProductCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 商品服务接口 —— 完整 REST 契约（方法签名 + 能力语义 + HTTP 映射的单一事实源）。
 *
 * <p>本接口承载 HTTP 映射与文档注解，契约 = 完整 REST 定义。服务端
 * {@code adapter.web.ProductController} 仅以 {@code @RestController} 标记并实现本接口；
 * 东西向内部查询同样经 HTTP 直连本契约的 REST 端点（消费方 RestClient 调用）。
 */
@Tag(name = "商品服务", description = "商品创建与查询")
@RequestMapping("/products")
public interface ProductService {

    /**
     * 创建商品。
     *
     * @param command 创建商品命令（名称 + 初始库存）
     * @return 创建后的商品信息
     */
    @Operation(summary = "创建商品", description = "新增商品并初始化库存")
    @PostMapping("")
    ProductCO createProduct(@Valid @RequestBody CreateProductCommand command);

    /**
     * 查询商品详情。
     *
     * @param productId 商品 ID
     * @return 商品完整信息
     */
    @Operation(summary = "查询商品详情", description = "根据 ID 获取商品信息")
    @GetMapping("/{productId}")
    ProductCO getProduct(
            @PathVariable("productId") @Parameter(description = "商品 ID") Long productId);
}
