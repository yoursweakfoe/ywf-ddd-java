package com.yoursweakfoe.common.ddd.fixtures.event;

import com.yoursweakfoe.common.ddd.domain.event.DomainEvent;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;

@Getter
public class OrderPlacedEvent extends DomainEvent {

    private final UUID orderId;
    private final BigDecimal totalAmount;

    public OrderPlacedEvent(UUID orderId, BigDecimal totalAmount) {
        super();
        this.orderId = orderId;
        this.totalAmount = totalAmount;
    }
}
