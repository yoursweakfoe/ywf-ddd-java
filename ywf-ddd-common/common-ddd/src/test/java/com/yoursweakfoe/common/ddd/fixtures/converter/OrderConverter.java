package com.yoursweakfoe.common.ddd.fixtures.converter;

import com.yoursweakfoe.common.ddd.fixtures.model.Order;
import com.yoursweakfoe.common.ddd.fixtures.model.OrderItem;
import com.yoursweakfoe.common.ddd.fixtures.model.OrderStatus;
import com.yoursweakfoe.common.ddd.fixtures.po.OrderPO;
import com.yoursweakfoe.common.ddd.infrastructure.converter.BasicConverter;
import java.util.List;
import java.util.UUID;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** 订单 Converter 测试夹具 —— 纯手写显式映射（订单项以 JSON 存储）。 */
public class OrderConverter implements BasicConverter<Order, OrderPO> {

    private static final JsonMapper MAPPER = new JsonMapper();

    @Override
    public Order toDomain(OrderPO po) {
        return Order.reconstitute(
                po.getId() != null ? UUID.fromString(po.getId()) : null,
                po.getStatus() != null ? OrderStatus.valueOf(po.getStatus()) : null,
                jsonToItems(po.getItems()),
                po.getTotalAmount(),
                po.getCustomerId());
    }

    @Override
    public OrderPO toPO(Order domain) {
        OrderPO po = new OrderPO();
        po.setId(domain.getId() != null ? domain.getId().toString() : null);
        po.setStatus(domain.getStatus() != null ? domain.getStatus().name() : null);
        po.setItems(itemsToJson(domain.getItems()));
        po.setTotalAmount(domain.getTotalAmount());
        po.setCustomerId(domain.getCustomerId());
        // createAt / updateAt 由 BasicAutoFillHandler 填充，不映射
        return po;
    }

    // ==================== JSON 序列化 ====================

    private static String itemsToJson(List<OrderItem> items) {
        if (items == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(items);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize order items", e);
        }
    }

    private static List<OrderItem> jsonToItems(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<OrderItem>>() {});
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to deserialize order items", e);
        }
    }
}
