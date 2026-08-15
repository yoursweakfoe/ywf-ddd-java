package com.yoursweakfoe.sampleapplication.sampleservice.application.order.assembler;

import com.yoursweakfoe.sampleapplication.sampleservice.application.order.dto.OrderViewDTO;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.OrderReadView;
import com.yoursweakfoe.common.ddd.application.assembler.BasicAssembler;
import org.springframework.stereotype.Component;

/**
 * 订单装配器 —— Domain → DTO 纯手写显式映射。
 *
 * <p>富领域模型：toDomain 不适用（Order 无 setter，需通过 reconstitute 重建），
 * 仅 toDTO 方向有效。字段增删时必须同步修改本类。
 */
@Component
public class OrderAssembler implements BasicAssembler<Order, OrderViewDTO> {

    @Override
    public OrderViewDTO toDTO(Order order) {
        OrderViewDTO dto = new OrderViewDTO();
        dto.setId(order.getId().toString());
        dto.setStatus(order.getStatus().name());
        dto.setItems(order.getItems().stream()
                .map(item -> new OrderViewDTO.OrderItemViewDTO(
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

    /**
     * 读模型 → DTO（读侧投影，绕过聚合根）。
     *
     * <p>读侧不 reconstitute 聚合根，故由读模型 {@link OrderReadView} 直接映射。
     * 乐观锁版本为写侧字段，读模型不承载，此处不填充。
     */
    public OrderViewDTO toDTO(OrderReadView view) {
        OrderViewDTO dto = new OrderViewDTO();
        dto.setId(view.id());
        dto.setStatus(view.status());
        dto.setItems(view.items().stream()
                .map(item -> new OrderViewDTO.OrderItemViewDTO(
                        item.productId(), item.quantity(), item.unitPrice()))
                .toList());
        dto.setTotalAmount(view.totalAmount());
        dto.setCustomerId(view.customerId());
        dto.setTrackingNumber(view.trackingNumber());
        dto.setCancelReason(view.cancelReason());
        dto.setCreateAt(view.createAt());
        dto.setUpdateAt(view.updateAt());
        return dto;
    }

    /** 富领域模型不支持 DTO → Domain 映射，使用 Order.reconstitute() 替代。 */
    @Override
    public Order toDomain(OrderViewDTO dto) {
        throw new UnsupportedOperationException("Rich domain model: use Order.reconstitute() instead");
    }

    /** 富领域模型不使用增量更新。 */
    @Override
    public void updateDomain(OrderViewDTO dto, Order domain) {
        throw new UnsupportedOperationException("Rich domain model: use reconstitute instead");
    }

    /** 富领域模型不使用增量更新。 */
    @Override
    public void updateDTO(Order domain, OrderViewDTO dto) {
        throw new UnsupportedOperationException("Rich domain model: use toDTO instead");
    }
}
