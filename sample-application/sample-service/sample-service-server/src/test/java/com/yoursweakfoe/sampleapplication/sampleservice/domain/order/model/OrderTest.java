package com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yoursweakfoe.common.exception.type.BusinessException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Order 聚合根单元测试 —— 状态机转换 + 不变量校验 + reconstitute。
 */
class OrderTest {

    private static final OrderItem ITEM = new OrderItem(1L, 2, BigDecimal.TEN);

    private Order createPendingOrder() {
        // 惰性重建出「未下单的 PENDING」——聚合自身行为方法的测试入口
        // （新建路径已收口至 OrderFactory：创建即 place()，不存在未下单的中间态）
        return Order.reconstitute(UUID.randomUUID(), OrderStatus.PENDING, List.of(ITEM),
                ITEM.subtotal(), "customer-1", null, null, null, null, 0);
    }

    // region 状态机合法路径

    @Test
    void place_shouldKeepPendingAndRegisterEvent() {
        Order order = createPendingOrder();
        order.place();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getDomainEvents()).hasSize(1);
    }

    @Test
    void pay_shouldTransitionFromPendingToPaid() {
        Order order = createPendingOrder();
        order.pay();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getDomainEvents()).hasSize(1);
    }

    @Test
    void confirm_shouldTransitionFromPaidToConfirmed() {
        Order order = createPendingOrder();
        order.pay();
        order.confirm();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void ship_shouldTransitionFromConfirmedToShipped() {
        Order order = createPendingOrder();
        order.pay();
        order.confirm();
        order.ship("TRACK-001");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(order.getTrackingNumber()).isEqualTo("TRACK-001");
    }

    @Test
    void deliver_shouldTransitionFromShippedToDelivered() {
        Order order = createPendingOrder();
        order.pay();
        order.confirm();
        order.ship("TRACK-001");
        order.deliver();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void complete_shouldTransitionFromDeliveredToCompleted() {
        Order order = createPendingOrder();
        order.pay();
        order.confirm();
        order.ship("TRACK-001");
        order.deliver();
        order.complete();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    void cancel_shouldWorkFromPending() {
        Order order = createPendingOrder();
        order.cancel("changed mind");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelReason()).isEqualTo("changed mind");
    }

    @Test
    void cancel_shouldWorkFromPaid() {
        Order order = createPendingOrder();
        order.pay();
        order.cancel("out of stock");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    // endregion

    // region 状态机非法路径

    @Test
    void pay_shouldThrowWhenNotPending() {
        Order order = createPendingOrder();
        order.pay();  // PENDING → PAID

        assertThatThrownBy(order::pay)
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void confirm_shouldThrowWhenNotPaid() {
        Order order = createPendingOrder();

        assertThatThrownBy(order::confirm)
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void ship_shouldThrowWhenNotConfirmed() {
        Order order = createPendingOrder();
        order.pay();

        assertThatThrownBy(() -> order.ship("TRACK"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void deliver_shouldThrowWhenNotShipped() {
        Order order = createPendingOrder();
        order.pay();
        order.confirm();

        assertThatThrownBy(order::deliver)
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void complete_shouldThrowWhenNotDelivered() {
        Order order = createPendingOrder();
        order.pay();
        order.confirm();
        order.ship("TRACK");

        assertThatThrownBy(order::complete)
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void cancel_shouldThrowFromConfirmed() {
        Order order = createPendingOrder();
        order.pay();
        order.confirm();

        assertThatThrownBy(() -> order.cancel("too late"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void cancel_shouldThrowFromShipped() {
        Order order = createPendingOrder();
        order.pay();
        order.confirm();
        order.ship("TRACK");

        assertThatThrownBy(() -> order.cancel("too late"))
                .isInstanceOf(BusinessException.class);
    }

    // endregion

    // region 不变量校验

    @Test
    void validate_shouldThrowWhenItemsEmpty() {
        Order order = Order.reconstitute(UUID.randomUUID(), OrderStatus.PENDING,
                List.of(), BigDecimal.ZERO, "customer-1", null, null, null, null, 0);

        assertThatThrownBy(order::validate)
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void validate_shouldThrowWhenCustomerIdNull() {
        Order order = Order.reconstitute(UUID.randomUUID(), OrderStatus.PENDING,
                List.of(ITEM), ITEM.subtotal(), null, null, null, null, null, 0);

        assertThatThrownBy(order::validate)
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void validate_shouldPassForValidOrder() {
        Order order = createPendingOrder();

        // 不抛异常即通过
        order.validate();
    }

    @Test
    void factory_shouldCalculateTotalAtCreation() {
        OrderItem item1 = new OrderItem(1L, 2, BigDecimal.TEN);   // 20
        OrderItem item2 = new OrderItem(2L, 1, new BigDecimal("5.50"));  // 5.50

        Order order = new OrderFactory().create("customer-1", List.of(item1, item2));

        assertThat(order.getTotalAmount()).isEqualByComparingTo(new BigDecimal("25.50"));
    }

    @Test
    void factory_shouldCreatePlacedOrder() {
        Order order = new OrderFactory().create("customer-1", List.of(ITEM));

        // 创建即合法：状态 PENDING + OrderPlacedEvent 已注册
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getDomainEvents()).hasSize(1);
    }

    @Test
    void factory_shouldRejectInvalidOrderAtCreation() {
        // 不变量违反在创建时即被拒绝——不存在「已构造未校验」的中间态
        assertThatThrownBy(() -> new OrderFactory().create("customer-1", List.of()))
                .isInstanceOf(BusinessException.class);

        assertThatThrownBy(() -> new OrderFactory().create(null, List.of(ITEM)))
                .isInstanceOf(BusinessException.class);
    }

    // endregion

    // region reconstitute

    @Test
    void reconstitute_shouldRestoreAllFields() {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        Order order = Order.reconstitute(
                id, OrderStatus.SHIPPED, List.of(ITEM),
                new BigDecimal("100.00"), "customer-1",
                "TRACK-999", null, now, now, 3);

        assertThat(order.getId()).isEqualTo(id);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(order.getCustomerId()).isEqualTo("customer-1");
        assertThat(order.getTrackingNumber()).isEqualTo("TRACK-999");
        assertThat(order.getVersion()).isEqualTo(3);
        assertThat(order.getDomainEvents()).isEmpty();
    }

    @Test
    void reconstitute_shouldNotRegisterEvents() {
        Order order = Order.reconstitute(
                UUID.randomUUID(), OrderStatus.PAID, List.of(ITEM),
                BigDecimal.TEN, "customer-1", null, null, null, null, 1);

        assertThat(order.getDomainEvents()).isEmpty();
    }

    // endregion

    // region 值对象

    @Test
    void orderItem_shouldCalculateSubtotal() {
        OrderItem item = new OrderItem(1L, 3, new BigDecimal("9.99"));

        assertThat(item.subtotal()).isEqualByComparingTo(new BigDecimal("29.97"));
    }

    @Test
    void orderItem_shouldThrowOnNullProductId() {
        assertThatThrownBy(() -> new OrderItem(null, 1, BigDecimal.TEN))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void orderItem_shouldThrowOnZeroQuantity() {
        assertThatThrownBy(() -> new OrderItem(1L, 0, BigDecimal.TEN))
                .isInstanceOf(BusinessException.class);
    }

    // endregion
}
