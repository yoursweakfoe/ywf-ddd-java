package com.yoursweakfoe.common.ddd.domain.event;

import java.util.List;

/**
 * 领域事件发布者接口 —— 定义领域事件发布的契约。
 *
 * <p>在 DDD 架构中，领域事件在聚合根内部注册， 在仓储层持久化成功后由 {@code DomainEventPublisher} 统一发布。 这确保了"先持久化，后发事件"的可靠顺序。
 *
 * <p>
 *
 * <h3>扩展点</h3>
 *
 * <ul>
 *   <li>进程内同步发布：使用 Spring {@code ApplicationEventPublisher}（默认实现）
 *   <li>异步发布：配合 {@code @Async} 或消息队列（Kafka / RabbitMQ）
 *   <li>事件存储：先写入事件表再发布，实现可靠事件溯源
 * </ul>
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

    /**
     * 批量发布领域事件。
     *
     * <p>默认实现逐个调用 {@link #publish(DomainEvent)}， 子类可覆写以实现批量优化。
     *
     * @param events 要发布的领域事件列表
     */
    default void publishAll(List<DomainEvent> events) {
        if (events != null) {
            events.forEach(this::publish);
        }
    }
}
