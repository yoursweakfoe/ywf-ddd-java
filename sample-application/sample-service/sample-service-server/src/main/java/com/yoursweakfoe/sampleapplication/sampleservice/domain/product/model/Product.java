package com.yoursweakfoe.sampleapplication.sampleservice.domain.product.model;

import com.yoursweakfoe.common.ddd.domain.model.AggregateRoot;
import com.yoursweakfoe.common.exception.type.BusinessException;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 商品聚合根 —— 管理商品基本信息、单价和库存。
 *
 * <p>库存扣减/回补通过行为方法完成，内聚业务校验。
 * 乐观锁由 PO 层 {@code @Version} 保护，领域层无需感知。
 *
 * <p>身份铸造收口于 {@link ProductFactory}（应用侧 UUIDv7，创建即合法）；
 * 业务构造器包私有，「谁能 new 一个商品」由包结构在编译期锁死。
 */
public class Product extends AggregateRoot<UUID> {

    // region 字段与构造器
    private UUID id;
    @Getter
    private String name;
    /** 商品单价（下单时订单项单价的唯一来源） */
    @Getter
    private BigDecimal price;
    @Getter
    private int stock;
    @Getter
    private OffsetDateTime createAt;
    @Getter
    private OffsetDateTime updateAt;
    @Getter
    private Integer version;

    /** 业务构造器（创建新商品）—— 包私有：创建路径收口于 {@link ProductFactory} */
    Product(UUID id, String name, BigDecimal price, int stock) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    /** 重建构造器（持久化层 Converter 使用，跳过校验） */
    public static Product reconstitute(UUID id, String name, BigDecimal price, int stock,
                                       OffsetDateTime createAt, OffsetDateTime updateAt,
                                       Integer version) {
        Product product = new Product(id, name, price, stock);
        product.createAt = createAt;
        product.updateAt = updateAt;
        product.version = version;
        return product;
    }

    @Override
    public UUID getId() {
        return id;
    }

    // endregion

    // region 行为方法

    /**
     * 扣减库存。
     *
     * @param quantity 扣减数量
     * @throws com.yoursweakfoe.common.exception.type.BusinessException 库存不足时
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
    }

    /**
     * 回补库存。
     *
     * @param quantity 回补数量
     */
    public void restoreStock(int quantity) {
        if (quantity <= 0) {
            throw new BusinessException("product:err.quantityMustBePositive");
        }
        this.stock += quantity;
    }
    // endregion

    // region 不变量校验

    @Override
    public void validate() {
        if (name == null) {
            throw new BusinessException("product:err.nameRequired");
        }
        if (price == null) {
            throw new BusinessException("product:err.priceRequired");
        }
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("product:err.priceNegative");
        }
        if (stock < 0) {
            throw new BusinessException("product:err.stockNegative");
        }
    }
    // endregion
}
