package com.yoursweakfoe.common.ddd.fixtures.event;

import com.yoursweakfoe.common.ddd.domain.event.domain.DomainEvent;
import lombok.Getter;

@Getter
public class StockDeductedEvent extends DomainEvent {

    private final Long productId;
    private final int deductedQuantity;

    public StockDeductedEvent(Long productId, int deductedQuantity) {
        super();
        this.productId = productId;
        this.deductedQuantity = deductedQuantity;
    }
}
