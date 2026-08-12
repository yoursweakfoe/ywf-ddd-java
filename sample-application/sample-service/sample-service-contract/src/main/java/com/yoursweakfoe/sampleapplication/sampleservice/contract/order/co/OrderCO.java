package com.yoursweakfoe.sampleapplication.sampleservice.contract.order.co;


import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 璁㈠崟 CO锛堝绾﹁緭鍑哄璞★級銆?*/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 璁㈠崟 ID */
    private String id;

    /** 璁㈠崟鐘舵€?*/
    private String status;

    /** 璁㈠崟椤瑰垪琛?*/
    private List<OrderItemCO> items;

    /** 璁㈠崟鎬婚噾棰?*/
    private BigDecimal totalAmount;

    /** 瀹㈡埛 ID */
    private String customerId;

    /** 鐗╂祦鍗曞彿锛堝彂璐у悗濉厖锛?*/
    private String trackingNumber;

    /** 鍙栨秷鍘熷洜锛堝彇娑堝悗濉厖锛?*/
    private String cancelReason;

    /** 璁㈠崟椤?CO銆?*/
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemCO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

        /** 鍟嗗搧 ID */
        private Long productId;

        /** 璐拱鏁伴噺 */
        private int quantity;

        /** 鍗曚环 */
        private BigDecimal unitPrice;
    }
}
