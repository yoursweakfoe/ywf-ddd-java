package com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model;

/**
 * 订单状态枚举。
 */
public enum OrderStatus {
    PENDING,    // 待支付
    PAID,       // 已支付
    CONFIRMED,  // 已确认（商家确认）
    SHIPPED,    // 已发货
    DELIVERED,  // 已签收
    COMPLETED,  // 已完成
    CANCELLED   // 已取消
}
