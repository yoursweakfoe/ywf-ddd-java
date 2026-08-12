package com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto;

import com.yoursweakfoe.common.contract.Command;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 鍙戣揣鍛戒护銆?*/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipOrderCommand implements Command, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 璁㈠崟 ID */
    private UUID orderId;

    /** 鐗╂祦鍗曞彿 */
    private String trackingNumber;
}
