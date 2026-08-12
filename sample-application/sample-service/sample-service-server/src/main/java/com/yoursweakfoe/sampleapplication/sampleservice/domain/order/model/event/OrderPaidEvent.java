package com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.event;

import com.yoursweakfoe.common.ddd.domain.event.DomainEvent;
import java.util.UUID;
import lombok.Getter;

/** 订单已支付事件 —— 订单支付成功后发布。 */
@Getter
public class OrderPaidEvent extends DomainEvent {

    /** 订单 ID */
    private final UUID orderId;

    public OrderPaidEvent(UUID orderId) {
        super();
        this.orderId = orderId;
    }
}
