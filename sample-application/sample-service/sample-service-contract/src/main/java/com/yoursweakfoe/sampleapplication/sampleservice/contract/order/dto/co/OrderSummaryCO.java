package com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.co;

import com.yoursweakfoe.common.contract.dto.co.CO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 订单概览 CO（列表页——精简字段，不含订单项明细）。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryCO implements CO, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "订单 ID")
    private String id;

    @Schema(description = "订单状态")
    private String status;

    @Schema(description = "订单总金额")
    private BigDecimal totalAmount;

    @Schema(description = "客户 ID")
    private String customerId;
}