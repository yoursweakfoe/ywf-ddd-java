package com.yoursweakfoe.common.ddd.fixtures;

import com.yoursweakfoe.common.ddd.fixtures.model.Order;
import com.yoursweakfoe.common.ddd.fixtures.model.OrderItem;
import com.yoursweakfoe.common.ddd.fixtures.model.OrderStatus;
import com.yoursweakfoe.common.ddd.fixtures.po.OrderPO;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class OrderFixtures {

    private OrderFixtures() {}

    public static Order createOrder() {
        return new Order(
                UUID.randomUUID(),
                OrderStatus.PENDING,
                List.of(new OrderItem("PROD-001", 2, new BigDecimal("29.99"))),
                new BigDecimal("59.98"),
                "CUST-001");
    }

    public static Order createOrderWithStatus(OrderStatus status) {
        return new Order(
                UUID.randomUUID(),
                status,
                List.of(new OrderItem("PROD-001", 2, new BigDecimal("29.99"))),
                new BigDecimal("59.98"),
                "CUST-001");
    }

    public static OrderPO createOrderPO() {
        OrderPO po = new OrderPO();
        po.setId(UUID.randomUUID().toString());
        po.setStatus(OrderStatus.PENDING.name());
        po.setItems("[{\"productId\":\"PROD-001\",\"quantity\":2,\"unitPrice\":29.99}]");
        po.setTotalAmount(new BigDecimal("59.98"));
        po.setCustomerId("CUST-001");
        po.setCreateAt(OffsetDateTime.now());
        po.setUpdateAt(OffsetDateTime.now());
        return po;
    }

    public static OrderPO createOrderPOWithStatus(OrderStatus status) {
        OrderPO po = createOrderPO();
        po.setStatus(status.name());
        return po;
    }
}
