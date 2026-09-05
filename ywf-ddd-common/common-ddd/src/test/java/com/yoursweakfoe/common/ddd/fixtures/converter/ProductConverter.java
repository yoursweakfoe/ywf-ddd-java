package com.yoursweakfoe.common.ddd.fixtures.converter;

import com.yoursweakfoe.common.ddd.fixtures.model.Product;
import com.yoursweakfoe.common.ddd.fixtures.po.ProductPO;
import com.yoursweakfoe.common.ddd.infrastructure.converter.BasicConverter;

/** 商品 Converter 测试夹具 —— 纯手写显式映射（version 只读透传，供 UPDATE 乐观锁条件消费）。 */
public class ProductConverter implements BasicConverter<Product, ProductPO> {

    @Override
    public Product toDomain(ProductPO po) {
        return Product.reconstitute(po.getId(), po.getName(), po.getStock(), po.getVersion());
    }

    @Override
    public ProductPO toPO(Product domain) {
        ProductPO po = new ProductPO();
        po.setId(domain.getId());
        po.setName(domain.getName());
        po.setStock(domain.getStock());
        po.setVersion(domain.getVersion());
        // createAt / updateAt 由 AuditFieldFiller 填充，不映射
        return po;
    }
}
