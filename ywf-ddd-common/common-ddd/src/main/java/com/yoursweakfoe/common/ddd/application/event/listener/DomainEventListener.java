package com.yoursweakfoe.common.ddd.application.event.listener;

import com.yoursweakfoe.common.ddd.domain.event.domain.DomainEvent;
import com.yoursweakfoe.common.ddd.domain.event.publisher.DomainEventPublisher;

/**
 * 域内反应监听器标记接口 —— 标识 application 层对领域事件（DomainEvent）作出进程内反应的监听器。
 *
 * <p>位于 {@code application/{aggregate}/event/listener/}，用 {@code @EventListener} /
 * {@code @TransactionalEventListener} 监听 Spring 进程内事件并按「接事件 → 加载聚合 →
 * 委托 DomainService / Publisher」的薄编排契约作出反应。本标记将这类组件显式定型为
 * <strong>域内反应</strong>，与 adapter 层处理外部集成事件的 Consumer 划清边界：
 * 本标记组件消费<strong>内部</strong>领域事件（Spring Event），adapter Consumer 消费
 * <strong>外部</strong>集成事件（MQ / Webhook）。
 *
 * <p>本接口为<strong>空标记</strong>：价值在「标识域内反应监听器身份」（供架构规则/ArchUnit
 * 识别与约束），而非约束方法签名——监听方法按事件类型自由命名（{@code onXxx}），
 * 签名由 Spring 事件路由机制决定，无统一形状可抽。
 *
 * <h3>为什么是空标记，而不是带方法的契约接口？</h3>
 * <ul>
 *   <li>监听逻辑由 Spring {@code @EventListener} 按事件类型路由，框架不消费监听器的方法，
 *       带方法只会制造「实现者被迫覆写」的样板。</li>
 *   <li>标记的价值在于<strong>定型角色</strong>：让「域内反应监听器」成为可被架构规则
 *       定位的类型，例如约束其不得被 AppService 直接依赖、必须位于 application 层。</li>
 * </ul>
 *
 * <h3>与相关类型的关系</h3>
 * <table>
 *   <tr><th>类型</th><th>层级</th><th>方向</th></tr>
 *   <tr><td>{@code DomainEventListener}（本接口）</td><td>application/event/listener</td><td>内部领域事件入（消费）</td></tr>
 *   <tr><td>{@link com.yoursweakfoe.common.ddd.application.event.publisher.IntegrationEventPublisher}</td><td>application/event/publisher</td><td>集成事件出（投递）</td></tr>
 *   <tr><td>{@link DomainEventPublisher}</td><td>domain/event（接口）→ infrastructure 实现</td><td>领域事件进程内发布（框架自动）</td></tr>
 *   <tr><td>adapter Consumer</td><td>adapter/event/consumer</td><td>集成事件入（MQ）</td></tr>
 * </table>
 *
 * @see DomainEvent
 * @see com.yoursweakfoe.common.ddd.application.event.publisher.IntegrationEventPublisher
 */
public interface DomainEventListener {
}
