package com.yoursweakfoe.sampleapplication.sampleservice.domain.order.event.domain;

import com.yoursweakfoe.common.ddd.domain.event.domain.DomainEvent;
import java.util.UUID;
import lombok.Getter;

/** 订单已确认事件 —— 商家确认订单后发布。 */
@Getter
public class OrderConfirmedEvent extends DomainEvent {

    /** 订单 ID */
    private final UUID orderId;

    public OrderConfirmedEvent(UUID orderId) {
        super();
        this.orderId = orderId;
    }
}
