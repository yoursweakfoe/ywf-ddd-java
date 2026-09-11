package com.yoursweakfoe.sampleapplication.sampleservice.application.order.presenter;

import com.yoursweakfoe.common.ddd.application.presenter.BasicPresenter;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.co.OrderCO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.enums.OrderStatus;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.dto.OrderDTO;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 订单写侧 Presenter —— 写侧 {@link OrderDTO} → {@link OrderCO} 单向呈现（契约输出清洗）。
 *
 * <p>写/读 Presenter 解耦：写侧由本类呈现 {@link OrderDTO}（含 version），读侧由
 * {@link OrderViewPresenter} 呈现 {@code OrderViewDTO}（不含 version）。决定外部消费方看到什么：
 * 审计字段（createdAt/updatedAt）、乐观锁版本（version）不映射即不暴露。
 *
 * <p>币种 → 原生值的唯一出契约拆箱位（案卷 2026-09-typed-identifier plan P-3；法卷锚 BP-6）：契约 CO 维持
 * 原生承载（id=String、productId=UUID），wire 形态与迁移前逐字节一致。
 */
@Component
public class OrderPresenter implements BasicPresenter<OrderDTO, OrderCO> {

    @Override
    public OrderCO present(OrderDTO dto) {
        OrderCO co = new OrderCO();
        co.setId(dto.getId().value().toString());
        // 内部 DTO 恒为 String（Assembler 映 domain.name()）；String→契约枚举在呈现层收口，
        // 奇偶守卫锁死值域、脏值当场 fail-fast，映射不外溢
        co.setStatus(OrderStatus.valueOf(dto.getStatus()));
        co.setItems(presentItems(dto.getItems()));
        co.setTotalAmount(dto.getTotalAmount());
        co.setCustomerId(dto.getCustomerId());
        co.setTrackingNumber(dto.getTrackingNumber());
        co.setCancelReason(dto.getCancelReason());
        // createdAt / updatedAt / version 为内部字段，不暴露给消费方
        return co;
    }

    private List<OrderCO.OrderItemCO> presentItems(List<OrderDTO.OrderItemDTO> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item -> new OrderCO.OrderItemCO(
                        item.getProductId().value(), item.getQuantity(), item.getUnitPrice()))
                .toList();
    }
}
