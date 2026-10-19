package com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.product.converter;

import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.id.ProductId;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.model.Product;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.product.mybatis.po.ProductPO;
import com.yoursweakfoe.common.ddd.infrastructure.converter.BasicConverter;
import org.springframework.stereotype.Component;

/**
 * 商品 Converter —— 纯手写显式映射（富领域模型）。
 *
 * <p>toDomain 通过 {@code Product.reconstitute()} 重建（PO 原生主键在此装箱为
 * {@link ProductId}，法卷锚 BP-13／案卷 2026-09-typed-identifier），不触发校验；
 * toPO 提取领域对象当前状态快照（身份在此拆箱回原生 UUID，PO 对币种零感知）。
 * 字段增删时必须同步修改本类并更新往返测试。
 *
 * <p>List/Set 集合方法由 {@code BasicConverter} default 实现提供。
 */
@Component
public class ProductConverter implements BasicConverter<Product, ProductPO> {

    @Override
    public Product toDomain(ProductPO po) {
        return Product.reconstitute(ProductId.of(po.getId()), po.getName(), po.getPrice(), po.getStock(),
                po.getCreatedAt(), po.getUpdatedAt(), po.getVersion());
    }

    @Override
    public ProductPO toPO(Product domain) {
        ProductPO po = new ProductPO();
        po.setId(domain.getId().value());
        po.setName(domain.getName());
        po.setPrice(domain.getPrice());
        po.setStock(domain.getStock());
        po.setVersion(domain.getVersion());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        // isDeleted 由 SQL 文本承担（INSERT 靠 DB 默认 FALSE、逻辑删除语句置位），不映射
        return po;
    }
}
