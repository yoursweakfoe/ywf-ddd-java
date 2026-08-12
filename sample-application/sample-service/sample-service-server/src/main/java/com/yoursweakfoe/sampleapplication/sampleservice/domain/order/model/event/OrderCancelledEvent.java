package com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.event;

import com.yoursweakfoe.common.ddd.domain.event.DomainEvent;
import java.util.UUID;
import lombok.Getter;

/** 订单取消事件 —— 订单被成功取消后发布。 */
@Getter
public class OrderCancelledEvent extends DomainEvent {

    /** 订单 ID */
    private final UUID orderId;
    /** 取消原因 */
    private final String reason;

    public OrderCancelledEvent(UUID orderId, String reason) {
        super();
        this.orderId = orderId;
        this.reason = reason;
    }
}
