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
 * 订单写侧内部视图 —— 写操作用例（CommandHandler）执行后，聚合根状态的完整投影。
 *
 * <p>与读侧 {@link OrderViewDTO} 解耦：写侧 DTO 承载乐观锁版本（version），
 * 供调用方感知写后状态（如乐观锁重试场景需回读新版本）；读侧 DTO 是纯读投影，
 * 不携带写侧关注点。二者不得互相复用，避免传统三层架构中"一个肥 DTO 贯穿所有层"
 * 的耦合。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO implements ApplicationDTO, Serializable {

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
    /** 乐观锁版本（写侧关注点，读侧不承载） */
    private Integer version;

    /** 订单项写侧视图。 */
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
