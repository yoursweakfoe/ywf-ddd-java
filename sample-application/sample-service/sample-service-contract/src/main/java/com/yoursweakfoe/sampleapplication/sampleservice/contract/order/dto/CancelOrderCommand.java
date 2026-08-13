package com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto;

import com.yoursweakfoe.common.contract.Command;
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
    private String orderId;

    /** 取消原因 */
    @NotBlank
    private String reason;
}
