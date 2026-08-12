package com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model;

import com.yoursweakfoe.common.ddd.domain.model.ValueObject;
import com.yoursweakfoe.common.exception.BusinessException;
import java.math.BigDecimal;

/**
 * 订单项值对象 —— 描述订单中一条商品明细。
 */
public record OrderItem(
        Long productId,
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
