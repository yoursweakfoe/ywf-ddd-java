package com.yoursweakfoe.sampleapplication.sampleservice.application.product.assembler;

import com.yoursweakfoe.sampleapplication.sampleservice.application.product.dto.ProductDTO;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.model.Product;
import com.yoursweakfoe.common.ddd.application.assembler.BasicAssembler;
import org.springframework.stereotype.Component;

/**
 * 商品装配器 —— 写侧 Domain → DTO（{@link ProductDTO}）纯手写显式映射。
 *
 * <p>富领域模型：toDomain 不适用（Product 无 setter，需通过 reconstitute 重建），
 * 仅 toDTO 方向有效。BasicAssembler 为最小契约（仅 toDomain/toDTO 与集合委托），
 * 不提供增量更新方法，富模型无需任何「不支持也要写 throw」的样板。字段增删时必须同步修改本类。
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
        dto.setStock(product.getStock());
        dto.setCreateAt(product.getCreateAt());
        dto.setUpdateAt(product.getUpdateAt());
        dto.setVersion(product.getVersion());
        return dto;
    }

    /** 富领域模型不支持 DTO → Domain 映射，使用 Product.reconstitute() 替代。 */
    @Override
    public Product toDomain(ProductDTO dto) {
        throw new UnsupportedOperationException("Rich domain model: use Product.reconstitute() instead");
    }
}
