package com.yoursweakfoe.sampleapplication.sampleservice.domain.product.event.domain;

import com.yoursweakfoe.common.ddd.domain.event.domain.DomainEvent;
import java.util.UUID;
import lombok.Getter;

/** 库存扣减事件 —— 商品库存被成功扣减后发布。 */
@Getter
public class StockDeductedEvent extends DomainEvent {

    /** 商品 ID */
    private final UUID productId;
    /** 扣减数量 */
    private final int quantity;

    public StockDeductedEvent(UUID productId, int quantity) {
        super();
        this.productId = productId;
        this.quantity = quantity;
    }
}
