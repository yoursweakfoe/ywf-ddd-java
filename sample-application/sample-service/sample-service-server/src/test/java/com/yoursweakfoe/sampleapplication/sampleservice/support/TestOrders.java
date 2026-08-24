package com.yoursweakfoe.sampleapplication.sampleservice.support;

import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.OrderFactory;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.OrderItem;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.OrderStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * 订单测试造数工具 —— 业务构造器收私有后的两条合法造数路径。
 *
 * <ul>
 *   <li>{@link #rebuilt(OrderStatus)} / {@link #rebuilt(UUID, OrderStatus)} —— 惰性重建：
 *       任意状态、零事件，Handler/Listener/Converter 测试用（不依赖状态机链路）</li>
 *   <li>{@link #placed()} —— 创建即合法：全新已下单订单，携带 OrderPlacedEvent，
 *       新建路径与工厂语义的测试用</li>
 * </ul>
 */
public final class TestOrders {

    /** 标准订单项：商品 1 × 2 件 × 10.00 = 小计 20.00 */
    public static final OrderItem ITEM = new OrderItem(1L, 2, BigDecimal.TEN);

    private static final String DEFAULT_CUSTOMER = "customer-1";

    private TestOrders() {
    }

    /** 随机 ID、指定状态的惰性重建订单（无事件）。 */
    public static Order rebuilt(OrderStatus status) {
        return rebuilt(UUID.randomUUID(), status);
    }

    /** 指定 ID、指定状态的惰性重建订单（无事件）。 */
    public static Order rebuilt(UUID id, OrderStatus status) {
        return Order.reconstitute(id, status, List.of(ITEM),
                ITEM.subtotal(), DEFAULT_CUSTOMER,
                null, null, null, null, 0);
    }

    /** 经工厂创建的全新已下单订单（携带 OrderPlacedEvent）。 */
    public static Order placed() {
        return new OrderFactory().create(DEFAULT_CUSTOMER, List.of(ITEM));
    }
}
