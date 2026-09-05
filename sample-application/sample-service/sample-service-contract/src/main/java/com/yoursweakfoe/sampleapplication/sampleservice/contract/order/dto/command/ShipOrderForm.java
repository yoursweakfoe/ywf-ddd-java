package com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 发货请求的绑定载体 —— <strong>非 CQE</strong>：只承载运输层字段。
 *
 * <p>运输形态为查询参数（{@code PUT /orders/{orderId}/ship?trackingNumber=...}，wire 格式自
 * {@code @RequestParam} 时代保持不变）；完整命令 {@link ShipOrderCommand} 由 Adapter 组装
 * （路径段 orderId 注入，与 {@link CancelOrderCommand} 的 orderId 注入同一模式）。
 * 长度上界对齐 schema：orders.orders.tracking_number VARCHAR(100)——绑定层校验（@Valid +
 * Bean Validation）把超长输入拦成 400，不再穿透到 DB 变 500 噪音。
 */
public record ShipOrderForm(

        @NotBlank
        @Size(max = 100)
        @Schema(description = "物流单号（≤100 字符，对齐 DB 列宽）") String trackingNumber
) {
}
