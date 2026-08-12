package com.yoursweakfoe.common.ddd.infrastructure.event;

import com.yoursweakfoe.common.ddd.domain.event.DomainEvent;
import com.yoursweakfoe.common.ddd.domain.event.DomainEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Spring 领域事件发布者 —— 基于 Spring {@link ApplicationEventPublisher} 的默认实现。
 *
 * <p>将领域事件桥接到 Spring 事件机制，使得 {@code @EventListener} 或 {@code @TransactionalEventListener}
 * 注解的方法能够接收和处理领域事件。
 *
 * <p>
 *
 * <h3>事件监听示例</h3>
 *
 * <pre>{@code
 * @Component
 * public class OrderEventHandler {
 *
 *     // 同步监听（事务内执行，抛异常会回滚主事务 —— 适合强一致副作用）
 *     @EventListener
 *     public void onOrderPlaced(OrderPlacedEvent event) {
 *         // 处理逻辑
 *     }
 *
 *     // 事务提交后执行（推荐用于通知、补偿、出站消息等副作用）
 *     // 注意：监听器内的数据库写入必须标注 @Transactional(propagation = REQUIRES_NEW)，
 *     // 因为原事务已完成，否则写入不会被提交
 *     @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
 *     @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
 *     public void onOrderPlacedAfterCommit(OrderPlacedEvent event) {
 *         // 发送通知
 *     }
 * }
 * }</pre>
 *
 * <p>
 *
 * <h3>异步扩展</h3>
 *
 * 如需异步处理事件，可在监听方法上添加 {@code @Async} 注解， 或替换本实现为基于消息队列的 Publisher。
 *
 * @see DomainEventPublisher
 * @see DomainEvent
 * @see ApplicationEventPublisher
 */
@Slf4j
@Component
public class SpringDomainEventPublisher implements DomainEventPublisher {

    // region 依赖注入
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 构造函数
     *
     * @param applicationEventPublisher Spring 事件发布器
     */
    public SpringDomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }
    // endregion

    /**
     * 通过 Spring 事件机制发布领域事件。
     *
     * <p>事件发布后，所有注册了对应事件类型的 {@code @EventListener} 方法 将被自动调用。
     *
     * @param event 要发布的领域事件
     */
    @Override
    public void publish(DomainEvent event) {
        if (event != null) {
            log.debug("Publishing domain event: {}", event);
            applicationEventPublisher.publishEvent(event);
        }
    }
}
