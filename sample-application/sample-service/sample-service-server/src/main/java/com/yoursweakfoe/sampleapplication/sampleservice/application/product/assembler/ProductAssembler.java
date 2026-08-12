package com.yoursweakfoe.sampleapplication.sampleservice.application.product.assembler;

import com.yoursweakfoe.sampleapplication.sampleservice.application.product.dto.ProductDTO;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.model.Product;
import com.yoursweakfoe.common.ddd.application.assembler.BasicAssembler;
import org.springframework.stereotype.Component;

/**
 * 商品装配器 —— Domain → DTO 纯手写显式映射。
 *
 * <p>富领域模型：toDomain 不适用（Product 无 setter，需通过 reconstitute 重建），
 * 仅 toDTO 方向有效。字段增删时必须同步修改本类。
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

    /** 富领域模型不使用增量更新。 */
    @Override
    public void updateDomain(ProductDTO dto, Product domain) {
        throw new UnsupportedOperationException("Rich domain model: use reconstitute instead");
    }

    /** 富领域模型不使用增量更新。 */
    @Override
    public void updateDTO(Product domain, ProductDTO dto) {
        throw new UnsupportedOperationException("Rich domain model: use toDTO instead");
    }
}
