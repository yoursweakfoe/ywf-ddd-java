package com.yoursweakfoe.common.ddd.application.event.subscriber;

/**
 * 领域事件订阅者标记接口 —— 标识 application 层对领域事件作出<strong>域内反应</strong>的监听组件。
 *
 * <p>位于 {@code application/{aggregate}/event/subscriber/}。实现类以 Spring
 * {@code @EventListener} / {@code @TransactionalEventListener} 监听
 * {@code DomainEventPublisher} 发布的进程内事件，按「接事件 → 加载聚合 → 委托
 * DomainService / Handler」的薄编排契约作出反应。本标记将这类组件显式定型为
 * <strong>域内反应</strong>，与 adapter 层消费外部集成事件的
 * {@code IntegrationEventSubscriber} 划清边界：本角色消费<strong>内部</strong>领域事件
 * （进程内），后者消费<strong>外部</strong>集成事件（MQ）。
 *
 * <p>本接口为<strong>空标记</strong>：监听方法由 Spring 按事件类型路由、命名自由（{@code onXxx}
 * 等），签名由事件类型决定，无统一方法形状可抽。标记的价值在角色定型与边界划定——
 * 本框架只提供事件角色词汇，不提供发布/订阅机制。
 *
 * <h3>与相关类型的关系</h3>
 * <table>
 *   <tr><th>类型</th><th>层级</th><th>方向</th></tr>
 *   <tr><td>{@code DomainEventSubscriber}（本接口）</td><td>application/event/subscriber</td><td>领域事件入（进程内）</td></tr>
 *   <tr><td>{@code DomainEventPublisher}</td><td>application/event/publisher</td><td>领域事件出（进程内，本角色的对偶）</td></tr>
 *   <tr><td>{@code IntegrationEventSubscriber}</td><td>adapter/event/subscriber</td><td>集成事件入（MQ）</td></tr>
 * </table>
 *
 * @see com.yoursweakfoe.common.ddd.domain.event.DomainEvent
 * @see com.yoursweakfoe.common.ddd.application.event.publisher.DomainEventPublisher
 */
public interface DomainEventSubscriber {
}
