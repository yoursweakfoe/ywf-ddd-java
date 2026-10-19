package com.yoursweakfoe.sampleapplication.sampleservice.support;

import com.yoursweakfoe.common.ddd.domain.model.AggregateIds;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.id.OrderId;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.OrderFactory;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.OrderItem;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.OrderStatus;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.id.ProductId;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * 订单测试造数工具 —— 业务构造器收私有后的两条合法造数路径。
 *
 * <ul>
 *   <li>{@link #rebuilt(OrderStatus)} / {@link #rebuilt(UUID, OrderStatus)} —— 惰性重建：
 *       任意状态，Handler/Converter 测试用（不依赖状态机链路）。签名保持裸 UUID 造数口，
 *       币种装箱在夹具内一处完成（{@code OrderId.of}，案卷 2026-09-typed-identifier）</li>
 *   <li>{@link #placed()} —— 创建即合法：全新已下单订单，
 *       新建路径与工厂语义的测试用</li>
 * </ul>
 */
public final class TestOrders {

    /** 标准订单项：商品 × 2 件 × 10.00 = 小计 20.00 */
    public static final OrderItem ITEM = new OrderItem(ProductId.of(UUID.randomUUID()), 2, BigDecimal.TEN);

    private static final String DEFAULT_CUSTOMER = "customer-1";

    private TestOrders() {
    }

    /** 铸造 ID（与生产同用的 v7 策略）、指定状态的惰性重建订单。 */
    public static Order rebuilt(OrderStatus status) {
        return rebuilt(AggregateIds.mint(), status);
    }

    /** 指定 ID（裸值造数口，装箱入币种）、指定状态的惰性重建订单。 */
    public static Order rebuilt(UUID id, OrderStatus status) {
        return Order.reconstitute(OrderId.of(id), status, List.of(ITEM),
                ITEM.subtotal(), DEFAULT_CUSTOMER,
                null, null, null, null, 0L);
    }

    /** 经工厂创建的全新已下单订单（创建即合法：校验 + place() 状态机推进）。 */
    public static Order placed() {
        return new OrderFactory().create(DEFAULT_CUSTOMER, List.of(ITEM));
    }
}
