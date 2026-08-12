package com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto;

import com.yoursweakfoe.common.contract.Command;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 涓嬪崟鍛戒护銆?*/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderCommand implements Command, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 瀹㈡埛 ID */
    private String customerId;

    /** 璁㈠崟椤瑰垪琛?*/
    private List<OrderItemDTO> items;

    /** 璁㈠崟椤?DTO锛堝懡浠ゅ唴宓岋級銆?*/
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

        /** 鍟嗗搧 ID */
        private Long productId;

        /** 璐拱鏁伴噺 */
        private int quantity;
    }
}
