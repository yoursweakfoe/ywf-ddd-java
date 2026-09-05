package com.yoursweakfoe.sampleapplication.sampleservice.application.product.assembler;

import com.yoursweakfoe.sampleapplication.sampleservice.application.product.dto.ProductDTO;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.model.Product;
import com.yoursweakfoe.common.ddd.application.assembler.BasicAssembler;
import org.springframework.stereotype.Component;

/**
 * 商品装配器 —— 写侧 Domain → DTO（{@link ProductDTO}）纯手写显式映射。
 *
 * <p>单向契约（仅 toDTO）：聚合构造恒走 {@code ProductFactory}（新建）/ {@code Product.reconstitute}
 * （存储重建）两扇门，教义全貌见 {@link BasicAssembler} 类 javadoc（单一事实源，此处不复述）。
 * 字段增删时必须同步修改本类。
 *
 * <p>读侧不经过本类：读路径绕过 domain，由 {@code ProductQueryRepository} 直接 PO → 读 DTO 投影。
 */
@Component
public class ProductAssembler implements BasicAssembler<Product, ProductDTO> {

    @Override
    public ProductDTO toDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setPrice(product.getPrice());
        dto.setStock(product.getStock());
        dto.setCreateAt(product.getCreateAt());
        dto.setUpdateAt(product.getUpdateAt());
        dto.setVersion(product.getVersion());
        return dto;
    }
}
