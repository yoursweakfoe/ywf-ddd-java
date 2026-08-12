package com.yoursweakfoe.sampleapplication.sampleservice.contract.order.enums;

import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.co.OrderCO;

/**
 * 订单状态枚举（契约共享）。
 *
 * <p>消费方通过此枚举解读 {@link OrderCO#getStatus()} 字段。
 */
public enum OrderStatus {

    /** 待支付 */
    PENDING,

    /** 已支付 */
    PAID,

    /** 已确认（商家确认） */
    CONFIRMED,

    /** 已发货 */
    SHIPPED,

    /** 已签收 */
    DELIVERED,

    /** 已完成 */
    COMPLETED,

    /** 已取消 */
    CANCELLED
}
