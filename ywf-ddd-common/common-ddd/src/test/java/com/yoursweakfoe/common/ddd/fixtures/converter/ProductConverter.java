package com.yoursweakfoe.common.ddd.fixtures.converter;

import com.yoursweakfoe.common.ddd.fixtures.model.Product;
import com.yoursweakfoe.common.ddd.fixtures.po.ProductPO;
import com.yoursweakfoe.common.ddd.infrastructure.converter.BasicConverter;

/** 商品 Converter 测试夹具 —— 纯手写显式映射。 */
public class ProductConverter implements BasicConverter<Product, ProductPO> {

    @Override
    public Product toDomain(ProductPO po) {
        return Product.reconstitute(po.getId(), po.getName(), po.getStock());
    }

    @Override
    public ProductPO toPO(Product domain) {
        ProductPO po = new ProductPO();
        po.setId(domain.getId());
        po.setName(domain.getName());
        po.setStock(domain.getStock());
        // version 由乐观锁拦截器维护，createAt / updateAt 由 BasicAutoFillHandler 填充，不映射
        return po;
    }

    @Override
    public void updateDomain(ProductPO po, Product domain) {
        domain.setId(po.getId());
        domain.setName(po.getName());
        domain.setStock(po.getStock());
    }

    @Override
    public void updatePO(Product domain, ProductPO po) {
        po.setId(domain.getId());
        po.setName(domain.getName());
        po.setStock(domain.getStock());
    }
}
