package com.yoursweakfoe.common.ddd.fixtures;

import com.yoursweakfoe.common.ddd.fixtures.model.Product;
import com.yoursweakfoe.common.ddd.fixtures.po.ProductPO;
import java.time.OffsetDateTime;

public final class ProductFixtures {

    private ProductFixtures() {}

    public static Product createProduct(int stock) {
        return new Product(null, "Test Product", stock);
    }

    public static ProductPO createProductPO() {
        ProductPO po = new ProductPO();
        po.setId(1L);
        po.setName("Test Product");
        po.setStock(100);
        po.setVersion(0);
        po.setCreateAt(OffsetDateTime.now());
        po.setUpdateAt(OffsetDateTime.now());
        return po;
    }
}
