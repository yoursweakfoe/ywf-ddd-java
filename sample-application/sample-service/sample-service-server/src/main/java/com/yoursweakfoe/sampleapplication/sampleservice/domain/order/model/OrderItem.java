package com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model;

import com.yoursweakfoe.common.ddd.domain.model.ValueObject;
import com.yoursweakfoe.common.exception.type.BusinessException;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.id.ProductId;
import java.math.BigDecimal;

/**
 * 订单项值对象 —— 描述订单中一条商品明细。
 *
 * <p>商品引用槽使用目标聚合币种 {@link ProductId}（跨聚合 import 仅准入 id 包，
 * 法卷锚 BP-14／案卷 2026-09-typed-identifier）。
 */
public record OrderItem(
        ProductId productId,
        int quantity,
        BigDecimal unitPrice
) implements ValueObject {

    public OrderItem {
        if (productId == null) {
            throw new BusinessException("order:err.productIdRequired");
        }
        if (quantity <= 0) {
            throw new BusinessException("order:err.quantityMustBePositive");
        }
        if (unitPrice == null) {
            throw new BusinessException("order:err.unitPriceRequired");
        }
    }

    /** 小计金额 = 单价 * 数量 */
    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
