package com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto;

import com.yoursweakfoe.common.contract.Command;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 瀹屾垚璁㈠崟鍛戒护銆?*/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompleteOrderCommand implements Command, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 璁㈠崟 ID */
    private UUID orderId;
}
