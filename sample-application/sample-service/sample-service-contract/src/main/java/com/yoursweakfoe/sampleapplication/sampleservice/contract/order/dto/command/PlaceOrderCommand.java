package com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command;

import com.yoursweakfoe.common.contract.dto.command.Command;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 下单命令。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderCommand implements Command, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 客户 ID */
    @NotBlank
    @Schema(description = "客户 ID")
    private String customerId;

    /** 订单项列表 */
    @NotEmpty
    @Schema(description = "订单项列表")
    private List<@Valid OrderItemView> items;

    /** 订单项 DTO（命令内嵌）。 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemView implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /** 商品 ID */
        @NotNull
        @Schema(description = "商品 ID")
        private UUID productId;

        /** 购买数量 */
        @Min(1)
        @Schema(description = "购买数量")
        private int quantity;
    }
}
