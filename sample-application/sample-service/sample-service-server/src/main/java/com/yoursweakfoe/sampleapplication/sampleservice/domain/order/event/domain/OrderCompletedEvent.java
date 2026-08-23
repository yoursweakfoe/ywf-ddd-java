package com.yoursweakfoe.sampleapplication.sampleservice.domain.order.event.domain;

import com.yoursweakfoe.common.ddd.domain.event.domain.DomainEvent;
import java.util.UUID;
import lombok.Getter;

/** 订单已完成事件 —— 订单流程结束后发布。 */
@Getter
public class OrderCompletedEvent extends DomainEvent {

    /** 订单 ID */
    private final UUID orderId;

    public OrderCompletedEvent(UUID orderId) {
        super();
        this.orderId = orderId;
    }
}
