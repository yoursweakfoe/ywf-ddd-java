package com.yoursweakfoe.common.ddd.fixtures.assembler;

import com.yoursweakfoe.common.ddd.fixtures.dto.ProductDTO;
import com.yoursweakfoe.common.ddd.fixtures.model.Product;
import com.yoursweakfoe.common.ddd.application.assembler.BasicAssembler;

/** 商品 Assembler 测试夹具 —— 纯手写显式映射（DTO 为不可变 record）。 */
public class ProductAssembler implements BasicAssembler<Product, ProductDTO> {

    @Override
    public Product toDomain(ProductDTO dto) {
        // DTO 不携带 version（内部乐观锁字段不进应用视图），按新实体初始值 0 重建
        return Product.reconstitute(dto.id(), dto.name(), dto.stock(), 0);
    }

    @Override
    public ProductDTO toDTO(Product domain) {
        return new ProductDTO(domain.getId(), domain.getName(), domain.getStock());
    }
}
