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
 * 进程内投递。集成事件（IntegrationEvent）的收发不在此包：出站由 application 层 {@code Publisher}
 * 翻译并经集成 Outbox 捕获、框架集成排空器投递（依赖 common-mq 时），入站由 adapter 层 {@code Consumer} 接收。
 *
 * <p><strong>投递时序（全链路 Outbox 规范）</strong>：本类由框架领域排空器（{@code OutboxRelay} 领域实例）
 * 在<strong>其自有事务内</strong>调用——事件从 {@code ddd_domain_event_outbox} 认领后于该事务中派发，
 * 监听器副作用 / 集成入箱 / 领域行标记完成三者原子提交。
 *
 * <p>
 *
 * <h3>事件监听示例</h3>
 *
 * <pre>{@code
 * @Component
 * public class OrderDomainEventListener {
 *
 *     // 派发生在排空器事务内：普通 @EventListener 即可
 *     @EventListener
 *     public void onOrderPlaced(OrderPlacedEvent event) {
 *         // 处理逻辑
 *     }
 *
 *     // 带数据库写入的副作用：普通 @Transactional（REQUIRED，加入排空器事务，原子提交）
 *     // 禁用 REQUIRES_NEW / @Async —— 会撕碎「副作用 + 集成入箱 + 标记完成」的原子性
 *     @EventListener
 *     @Transactional(rollbackFor = Exception.class)
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
 * 可靠化无需在此扩展——全链路 Transactional Outbox 的捕获与排空由
 * {@code infrastructure.event.outbox} 包提供（领域/集成两侧同事务入箱 + 框架排空器投递），对本类透明。
 * 监听器副作用须保持事务内，<strong>禁用</strong> {@code @Async}（会脱离排空器事务、撕碎原子性）。
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
