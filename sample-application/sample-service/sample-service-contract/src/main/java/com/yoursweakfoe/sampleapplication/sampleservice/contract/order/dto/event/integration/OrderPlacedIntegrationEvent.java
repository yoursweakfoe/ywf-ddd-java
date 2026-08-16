package com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.event.integration;

import com.yoursweakfoe.common.contract.dto.event.integration.IntegrationEvent;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单已下单集成事件（跨服务通知）。
 *
 * <p>由 Application 层监听 Domain Event（OrderPlacedEvent）后转换并发布到 MQ。
 * 消费方（其他微服务）通过订阅 MQ Topic 接收此事件。
 *
 * <p>注意：这是 Integration Event（对外契约），不是 Domain Event（领域内部）。
 * Domain Event 定义在 {@code domain/order/event/OrderPlacedEvent}，包含更丰富的领域细节。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlacedIntegrationEvent implements IntegrationEvent, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 订单 ID */
    private String orderId;

    /** 客户 ID */
    private String customerId;
}
