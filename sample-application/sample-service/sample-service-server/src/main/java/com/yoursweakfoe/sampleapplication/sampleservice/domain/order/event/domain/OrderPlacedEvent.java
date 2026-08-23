package com.yoursweakfoe.sampleapplication.sampleservice.domain.order.event.domain;

import com.yoursweakfoe.common.ddd.domain.event.domain.DomainEvent;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;

/** 订单已下单事件 —— 订单成功创建后发布。 */
@Getter
public class OrderPlacedEvent extends DomainEvent {

    /** 订单 ID */
    private final UUID orderId;
    /** 订单总金额 */
    private final BigDecimal totalAmount;
    /** 客户 ID */
    private final String customerId;

    public OrderPlacedEvent(UUID orderId, BigDecimal totalAmount, String customerId) {
        super();
        this.orderId = orderId;
        this.totalAmount = totalAmount;
        this.customerId = customerId;
    }
}
