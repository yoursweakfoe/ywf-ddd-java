package com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto;

import com.yoursweakfoe.common.contract.Query;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 鏌ヨ璁㈠崟璇︽儏銆?*/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetOrderQuery implements Query, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 璁㈠崟 ID */
    private String orderId;
}
