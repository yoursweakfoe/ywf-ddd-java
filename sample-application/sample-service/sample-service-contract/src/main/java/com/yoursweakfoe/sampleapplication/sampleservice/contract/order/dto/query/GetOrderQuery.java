package com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.query;

import com.yoursweakfoe.common.contract.dto.query.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 查询订单详情。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetOrderQuery implements Query, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 订单 ID（非法格式由 Web 层类型转换拦截 → 400） */
    @NotNull
    @Schema(description = "订单 ID")
    private UUID orderId;
}
