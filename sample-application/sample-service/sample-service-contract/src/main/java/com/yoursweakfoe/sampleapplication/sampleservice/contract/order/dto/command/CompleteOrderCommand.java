package com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command;

import com.yoursweakfoe.common.contract.dto.command.Command;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 完成订单命令。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompleteOrderCommand implements Command, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 订单 ID */
    @NotNull
    @Schema(description = "订单 ID")
    private UUID orderId;
}
