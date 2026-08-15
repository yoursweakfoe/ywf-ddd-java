package com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 订单读模型 —— 读侧投影专用，绕过聚合根。
 *
 * <p>读侧（QueryHandler）不 reconstitute 聚合根，由基础设施层直接由 PO 投影本读模型，
 * 因此仅承载读侧展示所需字段，不含乐观锁版本等写侧关注点。id / status 保留 PO 原文
 * （String），不还原为领域类型，避免引入写侧转换逻辑。
 *
 * @param id             订单 ID（PO 原文）
 * @param status         订单状态（PO 原文）
 * @param items          订单项列表
 * @param totalAmount    订单总金额
 * @param customerId     客户 ID
 * @param trackingNumber 物流单号
 * @param cancelReason   取消原因
 * @param createAt       创建时间
 * @param updateAt       更新时间
 */
public record OrderReadView(
        String id,
        String status,
        List<OrderItem> items,
        BigDecimal totalAmount,
        String customerId,
        String trackingNumber,
        String cancelReason,
        OffsetDateTime createAt,
        OffsetDateTime updateAt
) {
}
