package com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto;

import com.yoursweakfoe.common.contract.Query;
import jakarta.validation.constraints.NotBlank;

import java.io.Serial;
import java.io.Serializable;
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

    /** 订单 ID */
    @NotBlank
    private String orderId;
}
