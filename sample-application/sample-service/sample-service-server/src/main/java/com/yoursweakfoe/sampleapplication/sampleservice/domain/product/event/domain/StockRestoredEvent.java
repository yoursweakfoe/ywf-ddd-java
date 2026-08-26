package com.yoursweakfoe.sampleapplication.sampleservice.domain.product.event.domain;

import com.yoursweakfoe.common.ddd.domain.event.domain.DomainEvent;
import java.util.UUID;
import lombok.Getter;

/** 库存回补事件 —— 取消订单后库存被成功回补时发布。 */
@Getter
public class StockRestoredEvent extends DomainEvent {

    /** 商品 ID */
    private final UUID productId;
    /** 回补数量 */
    private final int restoredQuantity;

    public StockRestoredEvent(UUID productId, int restoredQuantity) {
        super();
        this.productId = productId;
        this.restoredQuantity = restoredQuantity;
    }
}
