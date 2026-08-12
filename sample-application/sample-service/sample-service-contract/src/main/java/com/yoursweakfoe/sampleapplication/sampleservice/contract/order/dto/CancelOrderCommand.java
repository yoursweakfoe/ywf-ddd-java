package com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto;

import com.yoursweakfoe.common.contract.Command;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 鍙栨秷璁㈠崟鍛戒护銆?*/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelOrderCommand implements Command, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 璁㈠崟 ID */
    private String orderId;

    /** 鍙栨秷鍘熷洜 */
    private String reason;
}
