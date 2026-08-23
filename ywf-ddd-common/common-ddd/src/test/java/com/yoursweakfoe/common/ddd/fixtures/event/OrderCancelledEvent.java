package com.yoursweakfoe.common.ddd.fixtures.event;

import com.yoursweakfoe.common.ddd.domain.event.domain.DomainEvent;
import java.util.UUID;
import lombok.Getter;

@Getter
public class OrderCancelledEvent extends DomainEvent {

    private final UUID orderId;
    private final String reason;

    public OrderCancelledEvent(UUID orderId, String reason) {
        super();
        this.orderId = orderId;
        this.reason = reason;
    }
}
