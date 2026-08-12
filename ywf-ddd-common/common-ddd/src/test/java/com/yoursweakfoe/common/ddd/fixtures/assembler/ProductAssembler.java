package com.yoursweakfoe.common.ddd.fixtures.assembler;

import com.yoursweakfoe.common.ddd.fixtures.dto.ProductDTO;
import com.yoursweakfoe.common.ddd.fixtures.model.Product;
import com.yoursweakfoe.common.ddd.application.assembler.BasicAssembler;

/** 商品 Assembler 测试夹具 —— 纯手写显式映射（DTO 为不可变 record）。 */
public class ProductAssembler implements BasicAssembler<Product, ProductDTO> {

    @Override
    public Product toDomain(ProductDTO dto) {
        return Product.reconstitute(dto.id(), dto.name(), dto.stock());
    }

    @Override
    public ProductDTO toDTO(Product domain) {
        return new ProductDTO(domain.getId(), domain.getName(), domain.getStock());
    }

    @Override
    public void updateDomain(ProductDTO dto, Product domain) {
        domain.setId(dto.id());
        domain.setName(dto.name());
        domain.setStock(dto.stock());
    }

    /** record DTO 不可变，不支持增量更新。 */
    @Override
    public void updateDTO(Product domain, ProductDTO dto) {
        throw new UnsupportedOperationException("Immutable record DTO: use toDTO instead");
    }
}
