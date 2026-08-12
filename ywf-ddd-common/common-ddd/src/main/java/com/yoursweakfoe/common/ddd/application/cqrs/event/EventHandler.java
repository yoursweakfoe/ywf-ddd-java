package com.yoursweakfoe.common.ddd.application.cqrs.event;

import com.yoursweakfoe.common.contract.Event;
import com.yoursweakfoe.common.ddd.application.cqrs.command.CommandHandler;
import com.yoursweakfoe.common.ddd.application.cqrs.query.QueryHandler;

/**
 * 事件处理器契约 —— 接收一个外部 Event 并执行响应逻辑。
 *
 * <p>每个外部事件（MQ 消息、微服务通知、Webhook）对应一个 EventHandler 实现，
 * 由应用层编排领域逻辑完成状态变更。事件处理无返回值。
 *
 * <p>基础设施层可基于此接口做统一 AOP 拦截（幂等校验、重试、死信处理等）。
 *
 * <h3>使用示例</h3>
 *
 * <pre>{@code
 * @Component
 * public class PaymentCompletedHandler implements EventHandler<PaymentCompletedEvent> {
 *     @Override
 *     public void handle(PaymentCompletedEvent event) {
 *         Order order = orderRepository.findById(event.getOrderId()).orElseThrow();
 *         order.markAsPaid(event.getTransactionId());
 *         orderRepository.save(order);
 *     }
 * }
 * }</pre>
 *
 * @param <E> 事件类型，必须实现 {@link Event}
 *
 * @see Event
 * @see CommandHandler
 * @see QueryHandler
 */
public interface EventHandler<E extends Event> {

    /**
     * 处理事件
     *
     * @param event 事件对象
     */
    void handle(E event);
}
