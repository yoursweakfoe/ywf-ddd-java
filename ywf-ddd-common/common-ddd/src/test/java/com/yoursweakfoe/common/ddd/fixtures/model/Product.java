package com.yoursweakfoe.common.ddd.fixtures.model;

import com.yoursweakfoe.common.ddd.domain.model.AggregateRoot;
import com.yoursweakfoe.common.exception.type.BusinessException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Product extends AggregateRoot<Long> {

    private Long id;
    private String name;
    private int stock;

    /** 乐观锁版本 —— 只读透传字段（不参与业务决策，仅供持久化层 UPDATE 的 WHERE 版本条件） */
    @Setter(AccessLevel.NONE)
    private Integer version;

    /** 新建产品（业务构造器） */
    public Product(Long id, String name, int stock) {
        this.id = id;
        this.name = name;
        this.stock = stock;
        this.version = 0; // 新实体初始版本（与 DDL DEFAULT 0 对齐）
    }

    /** 重建产品（持久化层 Converter 使用，跳过校验；version 从 PO 透传） */
    public static Product reconstitute(Long id, String name, int stock, Integer version) {
        Product product = new Product(id, name, stock);
        product.version = version;
        return product;
    }

    /** 扣减库存 */
    public void deductStock(int qty) {
        if (stock - qty <= 0) {
            throw new BusinessException("product:err.insufficientStock");
        }
        this.stock -= qty;
    }
}
