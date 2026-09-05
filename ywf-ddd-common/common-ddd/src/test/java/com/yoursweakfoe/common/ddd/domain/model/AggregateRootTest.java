package com.yoursweakfoe.common.ddd.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yoursweakfoe.common.ddd.fixtures.model.Order;
import com.yoursweakfoe.common.ddd.fixtures.model.OrderItem;
import com.yoursweakfoe.common.ddd.fixtures.model.OrderStatus;
import com.yoursweakfoe.common.ddd.fixtures.model.Product;
import com.yoursweakfoe.common.exception.type.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * AggregateRoot 测试 —— 不变量校验骨架。
 */
@DisplayName("AggregateRoot — 聚合根不变量校验测试")
class AggregateRootTest {

    private final UUID orderId = UUID.randomUUID();
    private final OrderItem sampleItem = new OrderItem("prod-1", 2, BigDecimal.valueOf(10));

    private Order validOrder() {
        return new Order(orderId, OrderStatus.PENDING, List.of(sampleItem), BigDecimal.TEN, "cust-1");
    }

    @Test
    void validate_defaultNoOp() {
        // Product 未覆写 validate()，调用应不抛
        Product product = new Product(1L, "Widget", 100);
        product.validate();
    }

    @Test
    void validate_orderWithEmptyItems_throws() {
        Order order = new Order(orderId, OrderStatus.PENDING, List.of(), BigDecimal.TEN, "cust-1");
        assertThatThrownBy(order::validate)
                .isInstanceOf(BusinessException.class)
                .hasMessage("order:err.itemsEmpty");
    }

    @Test
    void validate_orderWithZeroAmount_throws() {
        Order order = new Order(orderId, OrderStatus.PENDING, List.of(sampleItem), BigDecimal.ZERO, "cust-1");
        assertThatThrownBy(order::validate)
                .isInstanceOf(BusinessException.class)
                .hasMessage("order:err.totalMustBePositive");
    }

    @Test
    void validOrder_validate_passes() {
        validOrder().validate();
    }
}
