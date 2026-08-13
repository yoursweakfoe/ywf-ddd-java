package com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto;

import com.yoursweakfoe.common.contract.Command;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 确认订单命令。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmOrderCommand implements Command, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 订单 ID */
    @NotNull
    private UUID orderId;
}
