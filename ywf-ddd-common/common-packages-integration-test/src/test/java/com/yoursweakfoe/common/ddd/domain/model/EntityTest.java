package com.yoursweakfoe.common.ddd.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.yoursweakfoe.common.ddd.fixtures.model.Order;
import com.yoursweakfoe.common.ddd.fixtures.model.OrderStatus;
import com.yoursweakfoe.common.ddd.fixtures.model.Product;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Entity — 实体基于 ID 的身份判等测试")
class EntityTest {

    private final UUID id1 = UUID.randomUUID();
    private final UUID id2 = UUID.randomUUID();

    private Order orderWithId(UUID id) {
        return new Order(id, OrderStatus.PENDING, List.of(), BigDecimal.TEN, "cust-1");
    }

    @Test
    void entityEquals_sameId_sameType_true() {
        Order a = orderWithId(id1);
        Order b = orderWithId(id1);
        assertThat(a.entityEquals(b)).isTrue();
    }

    @Test
    void entityEquals_differentId_false() {
        Order a = orderWithId(id1);
        Order b = orderWithId(id2);
        assertThat(a.entityEquals(b)).isFalse();
    }

    @Test
    void entityEquals_nullId_false() {
        Order a = orderWithId(null);
        Order b = orderWithId(id1);
        assertThat(a.entityEquals(b)).isFalse();
    }

    @Test
    void entityEquals_sameInstance_true() {
        Order a = orderWithId(id1);
        assertThat(a.entityEquals(a)).isTrue();
    }

    @Test
    void entityEquals_differentType_false() {
        Order order = orderWithId(id1);
        Product product = new Product(1L, "Widget", 100);
        assertThat(order.entityEquals(product)).isFalse();
    }

    @Test
    void entityEquals_nullArg_false() {
        Order order = orderWithId(id1);
        assertThat(order.entityEquals(null)).isFalse();
    }
}
