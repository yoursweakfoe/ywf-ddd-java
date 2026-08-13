package com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto;

import com.yoursweakfoe.common.contract.Command;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 取消订单命令。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelOrderCommand implements Command, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 订单 ID */
    @NotBlank
    @Schema(description = "订单 ID")
    private String orderId;

    /** 取消原因 */
    @NotBlank
    @Schema(description = "取消原因")
    private String reason;
}
