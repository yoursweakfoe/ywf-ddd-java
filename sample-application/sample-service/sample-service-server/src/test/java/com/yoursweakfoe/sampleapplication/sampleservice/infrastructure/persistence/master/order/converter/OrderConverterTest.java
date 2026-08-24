package com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.OrderStatus;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.mybatisplus.po.OrderPO;
import com.yoursweakfoe.sampleapplication.sampleservice.support.TestOrders;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * OrderConverter 单元测试 —— 往返一致性 + JSON 序列化 + 异常路径。
 */
class OrderConverterTest {

    private final OrderConverter converter = new OrderConverter();

    private OrderPO buildOrderPO() {
        OrderPO po = new OrderPO();
        po.setId(UUID.randomUUID().toString());
        po.setStatus("PENDING");
        po.setItems("[{\"productId\":1,\"quantity\":2,\"unitPrice\":10.00}]");
        po.setTotalAmount(new BigDecimal("20.00"));
        po.setCustomerId("customer-1");
        po.setTrackingNumber(null);
        po.setCancelReason(null);
        po.setVersion(0);
        po.setCreateAt(OffsetDateTime.now());
        po.setUpdateAt(OffsetDateTime.now());
        po.setIsDelete(false);
        return po;
    }

    @Test
    void toDomain_shouldReconstituteOrder() {
        OrderPO po = buildOrderPO();

        Order order = converter.toDomain(po);

        assertThat(order.getId()).isEqualTo(UUID.fromString(po.getId()));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(order.getCustomerId()).isEqualTo("customer-1");
        assertThat(order.getVersion()).isEqualTo(0);
    }

    @Test
    void toPO_shouldExtractState() {
        Order order = TestOrders.rebuilt(OrderStatus.PENDING);

        OrderPO po = converter.toPO(order);

        assertThat(po.getId()).isEqualTo(order.getId().toString());
        assertThat(po.getStatus()).isEqualTo("PENDING");
        assertThat(po.getTotalAmount()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(po.getCustomerId()).isEqualTo("customer-1");
    }

    @Test
    void roundTrip_shouldPreserveData() {
        OrderPO original = buildOrderPO();

        Order domain = converter.toDomain(original);
        OrderPO result = converter.toPO(domain);

        assertThat(result.getId()).isEqualTo(original.getId());
        assertThat(result.getStatus()).isEqualTo(original.getStatus());
        assertThat(result.getTotalAmount()).isEqualByComparingTo(original.getTotalAmount());
        assertThat(result.getCustomerId()).isEqualTo(original.getCustomerId());
    }

    @Test
    void toDomain_shouldThrowOnInvalidJson() {
        OrderPO po = buildOrderPO();
        po.setItems("invalid-json{{{");

        assertThatThrownBy(() -> converter.toDomain(po))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to deserialize order items");
    }

    @Test
    void toDomain_shouldHandleEmptyItems() {
        OrderPO po = buildOrderPO();
        po.setItems("[]");

        Order order = converter.toDomain(po);

        assertThat(order.getItems()).isEmpty();
    }
}
