package com.yoursweakfoe.common.ddd.infrastructure.event.domain;

import com.yoursweakfoe.common.ddd.domain.event.domain.DomainEvent;
import com.yoursweakfoe.common.ddd.domain.event.publisher.DomainEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

/**
 * 进程内领域事件发布者 —— {@link DomainEventPublisher} 的默认实现，基于 Spring {@link ApplicationEventPublisher}。
 *
 * <p>将领域事件桥接到 Spring 事件机制，使得 {@code @EventListener} 注解的方法能够接收和处理领域事件。
 *
 * <p><strong>边界</strong>：本包（{@code infrastructure.event.domain}）仅负责<strong>领域事件</strong>的
 * 进程内投递。集成事件（IntegrationEvent）的收发不在此包：出站由 application 层 {@code Publisher} 投递
 * （依赖 common-mq），入站由 adapter 层 {@code Consumer} 接收。
 *
 * <p><strong>投递时序</strong>：上游 {@code DomainEventFlusher} 保证事件只在业务事务
 * <strong>提交之后</strong>（无活动事务的上下文）到达本类——无论经 Outbox 排空还是直发降级路径。
 *
 * <p>
 *
 * <h3>事件监听示例</h3>
 *
 * <pre>{@code
 * @Component
 * public class OrderDomainEventListener {
 *
 *     // 投递已在提交后完成：普通 @EventListener 即可，不应再用
 *     // @TransactionalEventListener(AFTER_COMMIT)（无事务可挂靠，默认不执行）
 *     @EventListener
 *     public void onOrderPlaced(OrderPlacedEvent event) {
 *         // 处理逻辑
 *     }
 *
 *     // 带数据库写入的副作用：自带独立事务（投递时无活动事务）
 *     @EventListener
 *     @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
 *     public void onOrderCancelled(OrderCancelledEvent event) {
 *         // 补偿写入（如库存回补）
 *     }
 * }
 * }</pre>
 *
 * <p>
 *
 * <h3>扩展</h3>
 *
 * <p>跨进程（MQ）发布请替换本实现为基于消息队列的 {@link DomainEventPublisher}（契约不变）。
 * 可靠化无需在此扩展——Transactional Outbox 的捕获由 {@code infrastructure.event.outbox} 包提供
 * （与业务同事务入箱），入箱后的排空投递归业务排空器，均对本类透明。
 * 异步处理可在监听方法上添加 {@code @Async} 注解。
 *
 * @see DomainEventPublisher
 * @see DomainEvent
 * @see ApplicationEventPublisher
 */
@Slf4j
public class InProcessDomainEventPublisher implements DomainEventPublisher {

    // region 依赖注入
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 构造函数
     *
     * @param applicationEventPublisher Spring 事件发布器
     */
    public InProcessDomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
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
