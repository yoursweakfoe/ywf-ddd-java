package com.yoursweakfoe.sampleapplication.sampleservice.application.order.presenter;

import com.yoursweakfoe.common.ddd.application.presenter.BasicPresenter;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.co.OrderCO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.co.OrderSummaryCO;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.dto.OrderViewDTO;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 订单 Presenter —— 内部 View → CO 单向呈现（契约输出清洗），纯手写显式映射。
 *
 * <p>决定外部消费方看到什么：审计字段（createAt/updateAt）、乐观锁版本（version）
 * 不映射即不暴露。presentList 由 {@code BasicPresenter} default 实现提供。
 *
 * <p>多视图演示：同一 OrderViewDTO 可呈现为 {@link OrderCO}（详情）或
 * {@link OrderSummaryCO}（概览），由调用方按场景选择。
 */
@Component
public class OrderPresenter implements BasicPresenter<OrderViewDTO, OrderCO> {

    @Override
    public OrderCO present(OrderViewDTO view) {
        OrderCO co = new OrderCO();
        co.setId(view.getId());
        co.setStatus(view.getStatus());
        co.setItems(presentItems(view.getItems()));
        co.setTotalAmount(view.getTotalAmount());
        co.setCustomerId(view.getCustomerId());
        co.setTrackingNumber(view.getTrackingNumber());
        co.setCancelReason(view.getCancelReason());
        // createAt / updateAt / version 为内部字段，不暴露给消费方
        return co;
    }

    /** 呈现为概览 CO（列表页，精简字段）。 */
    public OrderSummaryCO presentSummary(OrderViewDTO view) {
        OrderSummaryCO co = new OrderSummaryCO();
        co.setId(view.getId());
        co.setStatus(view.getStatus());
        co.setTotalAmount(view.getTotalAmount());
        co.setCustomerId(view.getCustomerId());
        return co;
    }

    private List<OrderCO.OrderItemCO> presentItems(List<OrderViewDTO.OrderItemViewDTO> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item -> new OrderCO.OrderItemCO(
                        item.getProductId(), item.getQuantity(), item.getUnitPrice()))
                .toList();
    }
}
