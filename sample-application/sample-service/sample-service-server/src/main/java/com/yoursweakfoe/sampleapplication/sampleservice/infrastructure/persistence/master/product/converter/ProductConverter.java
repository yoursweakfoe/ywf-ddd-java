package com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.product.converter;

import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.model.Product;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.product.mybatisplus.po.ProductPO;
import com.yoursweakfoe.common.ddd.infrastructure.converter.BasicConverter;
import org.springframework.stereotype.Component;

/**
 * 商品 Converter —— 纯手写显式映射（富领域模型）。
 *
 * <p>toDomain 通过 {@code Product.reconstitute()} 重建，不触发校验/事件；
 * toPO 提取领域对象当前状态快照。字段增删时必须同步修改本类并更新往返测试。
 *
 * <p>List/Set 集合方法由 {@code BasicConverter} default 实现提供。
 */
@Component
public class ProductConverter implements BasicConverter<Product, ProductPO> {

    @Override
    public Product toDomain(ProductPO po) {
        return Product.reconstitute(po.getId(), po.getName(), po.getPrice(), po.getStock(),
                po.getCreateAt(), po.getUpdateAt(), po.getVersion());
    }

    @Override
    public ProductPO toPO(Product domain) {
        ProductPO po = new ProductPO();
        po.setId(domain.getId());
        po.setName(domain.getName());
        po.setPrice(domain.getPrice());
        po.setStock(domain.getStock());
        po.setVersion(domain.getVersion());
        po.setCreateAt(domain.getCreateAt());
        po.setUpdateAt(domain.getUpdateAt());
        // isDelete 由 @TableLogic 逻辑删除维护，不映射
        return po;
    }
}
