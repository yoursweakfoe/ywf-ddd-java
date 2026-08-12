package com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.OrderItem;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.OrderStatus;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.po.OrderPO;
import com.yoursweakfoe.common.ddd.infrastructure.converter.BasicConverter;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * 订单 Converter —— 纯手写显式映射（富领域模型）。
 *
 * <p>toDomain 通过 {@code Order.reconstitute()} 重建，不触发校验/事件；
 * toPO 提取领域对象当前状态快照。字段增删时必须同步修改本类并更新往返测试。
 *
 * <p>订单项列表以 JSON 格式存储在 TEXT 列中。
 * List/Set 集合方法由 {@code BasicConverter} default 实现提供。
 */
@Component
public class OrderConverter implements BasicConverter<Order, OrderPO> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public Order toDomain(OrderPO po) {
        return Order.reconstitute(
                UUID.fromString(po.getId()),
                OrderStatus.valueOf(po.getStatus()),
                deserializeItems(po.getItems()),
                po.getTotalAmount(),
                po.getCustomerId(),
                po.getTrackingNumber(),
                po.getCancelReason(),
                po.getCreateAt(),
                po.getUpdateAt(),
                po.getVersion());
    }

    @Override
    public OrderPO toPO(Order domain) {
        OrderPO po = new OrderPO();
        po.setId(domain.getId().toString());
        po.setStatus(domain.getStatus().name());
        po.setItems(serializeItems(domain.getItems()));
        po.setTotalAmount(domain.getTotalAmount());
        po.setCustomerId(domain.getCustomerId());
        po.setTrackingNumber(domain.getTrackingNumber());
        po.setCancelReason(domain.getCancelReason());
        po.setVersion(domain.getVersion());
        po.setCreateAt(domain.getCreateAt());
        po.setUpdateAt(domain.getUpdateAt());
        // isDelete 由 @TableLogic 逻辑删除维护，不映射
        return po;
    }

    /** 富领域模型不使用增量更新，由 reconstitute 重建替代。 */
    @Override
    public void updateDomain(OrderPO po, Order domain) {
        throw new UnsupportedOperationException("Rich domain model: use reconstitute instead");
    }

    @Override
    public void updatePO(Order domain, OrderPO po) {
        po.setStatus(domain.getStatus().name());
        po.setItems(serializeItems(domain.getItems()));
        po.setTotalAmount(domain.getTotalAmount());
        po.setCustomerId(domain.getCustomerId());
        po.setTrackingNumber(domain.getTrackingNumber());
        po.setCancelReason(domain.getCancelReason());
        // id / version / createAt / updateAt / isDelete 由持久层机制维护，不合并
    }

    // ==================== JSON 序列化 ====================

    private String serializeItems(List<OrderItem> items) {
        try {
            return MAPPER.writeValueAsString(items);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize order items", e);
        }
    }

    private List<OrderItem> deserializeItems(String json) {
        try {
            return MAPPER.readValue(json, new TypeReference<List<OrderItem>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize order items", e);
        }
    }
}
