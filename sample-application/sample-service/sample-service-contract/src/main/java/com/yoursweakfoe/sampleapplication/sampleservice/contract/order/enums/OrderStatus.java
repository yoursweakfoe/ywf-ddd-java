package com.yoursweakfoe.sampleapplication.sampleservice.contract.order.enums;

/**
 * 订单状态枚举（契约共享）—— 值域的对外化身。
 *
 * <p>本枚举是 {@code OrderCO.status} / {@code OrderSummaryCO.status} 的字段类型与
 * {@code GetOrderPageQuery.status} 的过滤参数类型；wire 格式为常量名
 * （Jackson {@code name()}，自 String 字段枚举化前后 JSON 完全一致，零格式迁移）。
 * 值域镜像 domain 的 {@code OrderStatus} 状态机——模块隔离（R3/C1 双向封死
 * domain↔contract 引用方向）使两个同名枚举必须并存，漂移由
 * {@code ContractEnumParityTest} 奇偶守卫在构建期锁死。
 *
 * <p><strong>升级先于消费（已接受的契约纪律）</strong>：服务端新增状态值时，
 * 消费方必须先升级 contract jar 再消费新值——旧枚举遇未知字面量是硬失败
 * （反序列化/binding 异常），而非静默降级。这是本框架要的收紧方向。
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
