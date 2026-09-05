package com.yoursweakfoe.sampleapplication.sampleservice.application.order.assembler;

import com.yoursweakfoe.sampleapplication.sampleservice.application.order.dto.OrderDTO;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.common.ddd.application.assembler.BasicAssembler;
import org.springframework.stereotype.Component;

/**
 * 订单装配器 —— 写侧 Domain → DTO（{@link OrderDTO}）纯手写显式映射。
 *
 * <p>单向契约（仅 toDTO）：聚合构造恒走 {@code OrderFactory}（新建）/ {@code Order.reconstitute}
 * （存储重建）两扇门，教义全貌见 {@link BasicAssembler} 类 javadoc（单一事实源，此处不复述）。
 * 字段增删时必须同步修改本类。
 *
 * <p>读侧不经过本类：读路径绕过 domain，由 {@code OrderQueryRepository} 直接 PO → 读 DTO 投影。
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
}
