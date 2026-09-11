package com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.converter;

import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.id.OrderId;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.OrderItem;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.OrderStatus;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.id.ProductId;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.mybatis.po.OrderPO;
import com.yoursweakfoe.common.ddd.infrastructure.converter.BasicConverter;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * 订单 Converter —— 纯手写显式映射（富领域模型）。
 *
 * <p>toDomain 通过 {@code Order.reconstitute()} 重建（PO 原生主键在此装箱为
 * {@link OrderId}，法卷锚 BP-13／案卷 2026-09-typed-identifier），不触发校验；
 * toPO 提取领域对象当前状态快照（身份在此拆箱回原生 UUID，PO 对币种零感知）。
 * 字段增删时必须同步修改本类并更新往返测试。
 *
 * <p>订单项列表以 JSON 格式存储在 TEXT 列中。币种 {@code ProductId} <strong>不入
 * 持久 JSON 形态</strong>：序列化经本类私有中间形状 {@link ItemJson} 在 PO 边界拆装箱，
 * 落库字节形态（productId 为裸 UUID 字符串）与迁移前逐字节一致，存量行照读
 * （案卷裁 Q5「装载不站岗」与 wire 零破坏在此收口，不开全局 Jackson 配置）。
 * List/Set 集合方法由 {@code BasicConverter} default 实现提供。
 */
@Component
public class OrderConverter implements BasicConverter<Order, OrderPO> {

    private static final JsonMapper MAPPER = new JsonMapper();

    @Override
    public Order toDomain(OrderPO po) {
        return Order.reconstitute(
                OrderId.of(po.getId()),
                OrderStatus.valueOf(po.getStatus()),
                deserializeItems(po.getItems()),
                po.getTotalAmount(),
                po.getCustomerId(),
                po.getTrackingNumber(),
                po.getCancelReason(),
                po.getCreatedAt(),
                po.getUpdatedAt(),
                po.getVersion());
    }

    @Override
    public OrderPO toPO(Order domain) {
        OrderPO po = new OrderPO();
        po.setId(domain.getId().value());
        po.setStatus(domain.getStatus().name());
        po.setItems(serializeItems(domain.getItems()));
        po.setTotalAmount(domain.getTotalAmount());
        po.setCustomerId(domain.getCustomerId());
        po.setTrackingNumber(domain.getTrackingNumber());
        po.setCancelReason(domain.getCancelReason());
        po.setVersion(domain.getVersion());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        // isDeleted 由 SQL 文本承担（INSERT 靠 DB 默认 FALSE、逻辑删除语句置位），不映射
        return po;
    }

    // ==================== JSON 序列化 ====================

    /**
     * 订单项持久化中间形状 —— 与领域 {@link OrderItem} 同字段序、底值保持原生
     * （productId 裸 UUID），保证 JSON 列字节形态跨迁移不变。仅本类使用（PO 边界专用）。
     */
    private record ItemJson(UUID productId, int quantity, BigDecimal unitPrice) {
    }

    private String serializeItems(List<OrderItem> items) {
        try {
            List<ItemJson> wire = items.stream()
                    .map(item -> new ItemJson(item.productId().value(), item.quantity(), item.unitPrice()))
                    .toList();
            return MAPPER.writeValueAsString(wire);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize order items", e);
        }
    }

    /**
     * 反序列化订单项 JSON（写侧 reconstitute 专用）。
     *
     * <p>JSON 底值经 {@link ItemJson} 承接后装箱为币种 {@link ProductId}——
     * 存量行（v4/任意 UUID、裸字符串 productId）照过，不设版本位门槛。
     *
     * @param json JSON 字符串，可为 null / 空
     * @return 订单项列表，null / 空输入返回空列表
     */
    public List<OrderItem> deserializeItems(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        List<ItemJson> wire;
        try {
            wire = MAPPER.readValue(json, new TypeReference<List<ItemJson>>() {});
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to deserialize order items", e);
        }
        return wire.stream()
                .map(item -> new OrderItem(ProductId.of(item.productId()), item.quantity(), item.unitPrice()))
                .toList();
    }
}
