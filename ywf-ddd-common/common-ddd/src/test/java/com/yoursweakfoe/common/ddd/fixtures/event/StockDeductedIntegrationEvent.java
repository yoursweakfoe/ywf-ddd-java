package com.yoursweakfoe.common.ddd.fixtures.event;

import com.yoursweakfoe.common.contract.dto.event.integration.IntegrationEvent;
import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 库存扣减集成事件测试夹具 —— {@link IntegrationEvent} 的最小纯 POJO 实现，
 * 供 Outbox 捕获/排空测试验证「字段级序列化 + 行身份铸造 + 溯源血缘」。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockDeductedIntegrationEvent implements IntegrationEvent, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 商品 ID */
    private String productId;

    /** 扣减数量 */
    private int deductedQuantity;
}
