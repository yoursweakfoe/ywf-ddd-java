package com.yoursweakfoe.sampleapplication.sampleservice.application.order.dto;

import com.yoursweakfoe.common.ddd.application.dto.ApplicationDTO;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单读侧内部视图 —— 读操作用例（QueryHandler）的投影结果。
 *
 * <p>与写侧 {@link OrderDTO} 解耦：读侧 DTO 是纯读投影，不承载乐观锁版本等写侧关注点。
 * 读侧完全绕过 domain，由基础设施层直接从 PO 投影而来（不 reconstitute 聚合根、不建领域读模型）。
 * 二者不得互相复用，避免"一个肥 DTO 贯穿所有层"的耦合。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderViewDTO implements ApplicationDTO, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 订单 ID */
    private String id;
    /** 订单状态 */
    private String status;
    /** 订单项列表 */
    private List<OrderItemViewDTO> items;
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

    /** 订单项视图。 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemViewDTO implements Serializable {

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
