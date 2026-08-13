package com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto;

import com.yoursweakfoe.common.contract.Command;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
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
    private String customerId;

    /** 订单项列表 */
    @NotEmpty
    private List<@Valid OrderItemDTO> items;

    /** 订单项 DTO（命令内嵌）。 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDTO implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /** 商品 ID */
        @NotNull
        private Long productId;

        /** 购买数量 */
        @Min(1)
        private int quantity;
    }
}
