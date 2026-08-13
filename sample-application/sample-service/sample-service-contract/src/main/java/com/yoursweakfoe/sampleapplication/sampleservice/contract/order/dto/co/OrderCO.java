package com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.co;


import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 订单 CO（契约输出对象）。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 订单 ID */
    @Schema(description = "订单 ID")
    private String id;

    /** 订单状态 */
    @Schema(description = "订单状态")
    private String status;

    /** 订单项列表 */
    @Schema(description = "订单项列表")
    private List<OrderItemCO> items;

    /** 订单总金额 */
    @Schema(description = "订单总金额")
    private BigDecimal totalAmount;

    /** 客户 ID */
    @Schema(description = "客户 ID")
    private String customerId;

    /** 物流单号（发货后填充） */
    @Schema(description = "物流单号（发货后填充）")
    private String trackingNumber;

    /** 取消原因（取消后填充） */
    @Schema(description = "取消原因（取消后填充）")
    private String cancelReason;

    /** 订单项 CO。 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemCO implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /** 商品 ID */
        @Schema(description = "商品 ID")
        private Long productId;

        /** 购买数量 */
        @Schema(description = "购买数量")
        private int quantity;

        /** 单价 */
        @Schema(description = "单价")
        private BigDecimal unitPrice;
    }
}
