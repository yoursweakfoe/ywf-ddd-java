package com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.event;

import com.yoursweakfoe.common.ddd.domain.event.DomainEvent;
import java.util.UUID;
import lombok.Getter;

/** 订单已发货事件 —— 订单发货后发布。 */
@Getter
public class OrderShippedEvent extends DomainEvent {

    /** 订单 ID */
    private final UUID orderId;
    /** 物流单号 */
    private final String trackingNumber;

    public OrderShippedEvent(UUID orderId, String trackingNumber) {
        super();
        this.orderId = orderId;
        this.trackingNumber = trackingNumber;
    }
}
