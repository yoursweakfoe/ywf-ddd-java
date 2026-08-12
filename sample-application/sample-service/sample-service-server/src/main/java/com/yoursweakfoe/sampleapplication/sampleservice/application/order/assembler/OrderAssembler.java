package com.yoursweakfoe.sampleapplication.sampleservice.application.order.assembler;

import com.yoursweakfoe.sampleapplication.sampleservice.application.order.dto.OrderDTO;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.common.ddd.application.assembler.BasicAssembler;
import org.springframework.stereotype.Component;

/**
 * 订单装配器 —— Domain → DTO 纯手写显式映射。
 *
 * <p>富领域模型：toDomain 不适用（Order 无 setter，需通过 reconstitute 重建），
 * 仅 toDTO 方向有效。字段增删时必须同步修改本类。
 */
@Component
public class OrderAssembler implements BasicAssembler<Order, OrderDTO> {

    @Override
    public OrderDTO toDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId().toString());
        dto.setStatus(order.getStatus().name());
        dto.setItems(order.getItems().stream()
                .map(item -> new OrderDTO.OrderItemDTO(
                        item.productId(), item.quantity(), item.unitPrice()))
                .toList());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setCustomerId(order.getCustomerId());
        dto.setTrackingNumber(order.getTrackingNumber());
        dto.setCancelReason(order.getCancelReason());
        dto.setCreateAt(order.getCreateAt());
        dto.setUpdateAt(order.getUpdateAt());
        dto.setVersion(order.getVersion());
        return dto;
    }

    /** 富领域模型不支持 DTO → Domain 映射，使用 Order.reconstitute() 替代。 */
    @Override
    public Order toDomain(OrderDTO dto) {
        throw new UnsupportedOperationException("Rich domain model: use Order.reconstitute() instead");
    }

    /** 富领域模型不使用增量更新。 */
    @Override
    public void updateDomain(OrderDTO dto, Order domain) {
        throw new UnsupportedOperationException("Rich domain model: use reconstitute instead");
    }

    /** 富领域模型不使用增量更新。 */
    @Override
    public void updateDTO(Order domain, OrderDTO dto) {
        throw new UnsupportedOperationException("Rich domain model: use toDTO instead");
    }
}
