package com.yoursweakfoe.common.ddd.fixtures.model;

import com.yoursweakfoe.common.ddd.domain.model.AggregateRoot;
import com.yoursweakfoe.common.exception.type.BusinessException;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Product extends AggregateRoot<Long> {

    private Long id;
    private String name;
    private int stock;

    /** 新建产品（业务构造器） */
    public Product(Long id, String name, int stock) {
        this.id = id;
        this.name = name;
        this.stock = stock;
    }

    /** 重建产品（持久化层 Converter 使用，跳过校验） */
    public static Product reconstitute(Long id, String name, int stock) {
        return new Product(id, name, stock);
    }

    /** 扣减库存 */
    public void deductStock(int qty) {
        if (stock - qty <= 0) {
            throw new BusinessException("product:err.insufficientStock");
        }
        this.stock -= qty;
    }
}
