package com.yoursweakfoe.sampleapplication.sampleservice.application.order.dto;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 订单 DTO（内部视图）。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 订单 ID */
    private String id;
    /** 订单状态 */
    private String status;
    /** 订单项列表 */
    private List<OrderItemDTO> items;
    /** 订单总金额 */
    private BigDecimal totalAmount;
    /** 客户 ID */
    private String customerId;
    /** 物流单号 */
    private String trackingNumber;
    /** 取消原因 */
    private String cancelReason;
    /** 创建时间 */
    private OffsetDateTime createAt;
    /** 更新时间 */
    private OffsetDateTime updateAt;
    /** 乐观锁版本 */
    private Integer version;

    /** 订单项 DTO。 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

        /** 商品 ID */
        private Long productId;
        /** 购买数量 */
        private int quantity;
        /** 单价 */
        private BigDecimal unitPrice;
    }
}
