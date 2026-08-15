package com.yoursweakfoe.sampleapplication.sampleservice.application.order.presenter;

import com.yoursweakfoe.common.ddd.application.presenter.BasicPresenter;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.co.OrderCO;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.dto.OrderDTO;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 订单写侧 Presenter —— 写侧 {@link OrderDTO} → {@link OrderCO} 单向呈现（契约输出清洗）。
 *
 * <p>写/读 Presenter 解耦：写侧由本类呈现 {@link OrderDTO}（含 version），读侧由
 * {@link OrderViewPresenter} 呈现 {@code OrderViewDTO}（不含 version）。决定外部消费方看到什么：
 * 审计字段（createAt/updateAt）、乐观锁版本（version）不映射即不暴露。
 */
@Component
public class OrderPresenter implements BasicPresenter<OrderDTO, OrderCO> {

    @Override
    public OrderCO present(OrderDTO dto) {
        OrderCO co = new OrderCO();
        co.setId(dto.getId());
        co.setStatus(dto.getStatus());
        co.setItems(presentItems(dto.getItems()));
        co.setTotalAmount(dto.getTotalAmount());
        co.setCustomerId(dto.getCustomerId());
        co.setTrackingNumber(dto.getTrackingNumber());
        co.setCancelReason(dto.getCancelReason());
        // createAt / updateAt / version 为内部字段，不暴露给消费方
        return co;
    }

    private List<OrderCO.OrderItemCO> presentItems(List<OrderDTO.OrderItemDTO> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item -> new OrderCO.OrderItemCO(
                        item.getProductId(), item.getQuantity(), item.getUnitPrice()))
                .toList();
    }
}
