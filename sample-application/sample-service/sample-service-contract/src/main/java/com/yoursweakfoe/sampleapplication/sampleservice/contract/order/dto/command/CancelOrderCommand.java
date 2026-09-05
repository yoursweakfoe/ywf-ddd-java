package com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command;

import com.yoursweakfoe.common.contract.dto.command.Command;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 取消订单命令。
 *
 * <p>订单 ID 的唯一事实源是 REST 路径参数（{@code PUT /orders/{orderId}/cancel}），
 * 由 Adapter 组装命令时注入，客户端请求体只需携带 {@code reason}——
 * 避免身份在路径与请求体中双源并存、不一致时静默歧义。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelOrderCommand implements Command, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 订单 ID（由 Adapter 从路径参数注入，客户端无需传递） */
    @Schema(hidden = true)
    private UUID orderId;

    /** 取消原因（上界对齐 orders.orders.cancel_reason VARCHAR(500)） */
    @NotBlank
    @Size(max = 500)
    @Schema(description = "取消原因（≤500 字符）")
    private String reason;
}
