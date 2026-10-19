package com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.id.OrderId;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.OrderStatus;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.id.ProductId;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.mybatis.po.OrderPO;
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
        po.setId(UUID.randomUUID());
        po.setStatus("PENDING");
        po.setItems("[{\"productId\":\"550e8400-e29b-41d4-a716-446655440000\",\"quantity\":2,\"unitPrice\":10.00}]");
        po.setTotalAmount(new BigDecimal("20.00"));
        po.setCustomerId("customer-1");
        po.setTrackingNumber(null);
        po.setCancelReason(null);
        po.setVersion(0L);
        po.setCreatedAt(OffsetDateTime.now());
        po.setUpdatedAt(OffsetDateTime.now());
        po.setIsDeleted(false);
        return po;
    }

    @Test
    void toDomain_shouldReconstituteOrder() {
        OrderPO po = buildOrderPO();

        Order order = converter.toDomain(po);

        assertThat(order.getId().value()).isEqualTo(po.getId());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(order.getCustomerId()).isEqualTo("customer-1");
        assertThat(order.getVersion()).isEqualTo(0L);
    }

    @Test
    void toPO_shouldExtractState() {
        Order order = TestOrders.rebuilt(OrderStatus.PENDING);

        OrderPO po = converter.toPO(order);

        assertThat(po.getId()).isEqualTo(order.getId().value());
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
        // items JSON 字节形态守卫：币种 ProductId 不入持久 JSON（案卷 2026-09-typed-identifier，
        // Converter 私有 ItemJson 中间形状在 PO 边界拆装箱，落库形态与迁移前逐字节一致）
        assertThat(result.getItems()).isEqualTo(original.getItems());
    }

    /**
     * 装载路径不校验版本位（案卷 2026-09-typed-identifier 裁 Q5「只装箱不站岗」的反证）：
     * 存量行/手工插入行携非 v7（此处 v4）UUID，经 {@code OrderId.of} / {@code ProductId.of}
     * 与 {@code toDomain} 装载照样通过，且回写 JSON 字节形态不变。
     */
    @Test
    void acceptsAnyUuidOnLoad() {
        UUID legacyV4 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        assertThat(legacyV4.version()).isNotEqualTo(7); // 前置：夹具底值确为非 v7

        // ① 装箱零门槛：of() 仅 null 检查，不查格式、不查版本位
        assertThatCode(() -> OrderId.of(legacyV4)).doesNotThrowAnyException();
        assertThatCode(() -> ProductId.of(legacyV4)).doesNotThrowAnyException();

        // ② 装载往返：v4 主键 + v4 商品 ID 的存量行照过 toDomain（buildOrderPO 的 items JSON
        //    本就携 v4 商品 ID——历史形状），reconstitute 不触发任何底值校验
        OrderPO po = buildOrderPO();
        po.setId(legacyV4);
        Order order = converter.toDomain(po);
        assertThat(order.getId()).isEqualTo(OrderId.of(legacyV4));
        assertThat(order.getItems().get(0).productId()).isEqualTo(ProductId.of(legacyV4));

        // ③ 回写：PO 主键与 items JSON 均维持原生字节形态（wire 零破坏）
        OrderPO back = converter.toPO(order);
        assertThat(back.getId()).isEqualTo(legacyV4);
        assertThat(back.getItems()).isEqualTo(po.getItems());
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
