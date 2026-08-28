package com.yoursweakfoe.common.ddd.domain.event.publisher;

import com.yoursweakfoe.common.ddd.domain.event.domain.DomainEvent;

/**
 * 领域事件发布者接口 —— 定义领域事件进程内派发的契约。
 *
 * <p><strong>调用时机（全链路 Outbox 规范）</strong>：本端口仅由框架领域排空器
 * （{@code OutboxRelay} 领域实例）在排空事务内调用；事件先经
 * {@code DomainEventCapture} 与业务同事务入箱，此处是投递时刻而非捕获时刻。
 *
 * <p><strong>边界</strong>：本接口仅定义领域事件的<strong>进程内</strong>派发契约。
 * 集成事件（IntegrationEvent）的收发不经过本接口：出站由 application 层 Capture
 * 翻译并经集成 Outbox 捕获、框架集成排空器投递，入站由 adapter 层 Consumer 接收。
 *
 * @see DomainEvent
 * @see com.yoursweakfoe.common.ddd.domain.model.AggregateRoot
 */
public interface DomainEventPublisher {

    /**
     * 发布单个领域事件。
     *
     * @param event 要发布的领域事件
     */
    void publish(DomainEvent event);
}
