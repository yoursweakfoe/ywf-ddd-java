package com.yoursweakfoe.sampleapplication.sampleservice.domain.product.model;

import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.model.event.StockDeductedEvent;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.model.event.StockRestoredEvent;
import com.yoursweakfoe.common.ddd.domain.model.AggregateRoot;
import com.yoursweakfoe.common.exception.BusinessException;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 商品聚合根 —— 管理商品基本信息和库存。
 *
 * <p>库存扣减/回补通过行为方法完成，内聚业务校验。
 * 乐观锁由 PO 层 {@code @Version} 保护，领域层无需感知。
 */
public class Product extends AggregateRoot<Long> {

    // region 字段与构造器
    private Long id;
    @Getter
    private String name;
    @Getter
    private int stock;
    @Getter
    private OffsetDateTime createAt;
    @Getter
    private OffsetDateTime updateAt;
    @Getter
    private Integer version;

    /** 业务构造器（创建新商品） */
    public Product(Long id, String name, int stock) {
        this.id = id;
        this.name = name;
        this.stock = stock;
    }

    /** 重建构造器（持久化层 Converter 使用，跳过校验与事件注册） */
    public static Product reconstitute(Long id, String name, int stock,
                                       OffsetDateTime createAt, OffsetDateTime updateAt,
                                       Integer version) {
        Product product = new Product(id, name, stock);
        product.createAt = createAt;
        product.updateAt = updateAt;
        product.version = version;
        return product;
    }

    @Override
    public Long getId() {
        return id;
    }

    // endregion

    // region 行为方法

    /**
     * 扣减库存。
     *
     * @param quantity 扣减数量
     * @throws com.yoursweakfoe.common.exception.BusinessException 库存不足时
     */
    public void deductStock(int quantity) {
        if (quantity <= 0) {
            throw new BusinessException("product:err.quantityMustBePositive");
        }
        if (stock < quantity) {
            throw new BusinessException("product:err.insufficientStock",
                    Map.of("productId", id, "required", quantity, "available", stock));
        }
        this.stock -= quantity;
        registerEvent(new StockDeductedEvent(id, quantity));
    }

    /**
     * 回补库存（取消订单时调用，不注册事件）。
     *
     * @param quantity 回补数量
     */
    public void replenishStock(int quantity) {
        if (quantity <= 0) {
            throw new BusinessException("product:err.quantityMustBePositive");
        }
        this.stock += quantity;
    }

    /**
     * 回补库存并注册领域事件。
     *
     * @param quantity 回补数量
     */
    public void restoreStock(int quantity) {
        if (quantity <= 0) {
            throw new BusinessException("product:err.quantityMustBePositive");
        }
        this.stock += quantity;
        registerEvent(new StockRestoredEvent(this.id, quantity));
    }
    // endregion

    // region 不变量校验

    @Override
    public void validate() {
        if (name == null) {
            throw new BusinessException("product:err.nameRequired");
        }
        if (stock < 0) {
            throw new BusinessException("product:err.stockNegative");
        }
    }
    // endregion
}
