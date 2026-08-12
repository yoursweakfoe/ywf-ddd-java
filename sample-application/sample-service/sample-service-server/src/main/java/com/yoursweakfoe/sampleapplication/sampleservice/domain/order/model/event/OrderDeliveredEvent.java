package com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.event;

import com.yoursweakfoe.common.ddd.domain.event.DomainEvent;
import java.util.UUID;
import lombok.Getter;

/** 订单已签收事件 —— 买家签收后发布。 */
@Getter
public class OrderDeliveredEvent extends DomainEvent {

    /** 订单 ID */
    private final UUID orderId;

    public OrderDeliveredEvent(UUID orderId) {
        super();
        this.orderId = orderId;
    }
}
