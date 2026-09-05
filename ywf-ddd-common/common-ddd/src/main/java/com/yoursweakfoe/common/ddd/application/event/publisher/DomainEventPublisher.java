package com.yoursweakfoe.common.ddd.application.event.publisher;

/**
 * 领域事件发布者标记接口 —— 标识 application 层「进程内发布领域事件」的组件角色。
 *
 * <p>位于 {@code application/{aggregate}/event/publisher/}。职责是把领域内已发生的事实
 * （{@code DomainEvent}）通知给进程内订阅方——典型实现是对 Spring
 * {@code ApplicationEventPublisher} 的薄包装；发布时机（事务内 / 提交后）属业务决策。
 * 本框架<strong>只提供角色词汇、不提供发布机制</strong>，实现本标记即定型该身份。
 *
 * <p><strong>边界</strong>：本角色仅负责领域事件的<strong>进程内</strong>发布；跨服务边界的
 * 通知由同包 {@link IntegrationEventPublisher} 承担（翻译为集成事件后经业务自持的消息
 * 中间件出站）。
 *
 * <p>本接口为<strong>空标记</strong>：发布方法的命名与签名由所选机制决定（Spring 接收任意
 * 事件对象），无统一方法形状可抽——强加方法只会制造实现者被迫覆写的样板。标记的价值在
 * 角色定型：包位置 + 类型 grep 即可锚定全部事件出口。
 *
 * <h3>与相关类型的关系</h3>
 * <table>
 *   <tr><th>类型</th><th>层级</th><th>方向</th></tr>
 *   <tr><td>{@code DomainEventPublisher}（本接口）</td><td>application/event/publisher</td><td>领域事件出（进程内）</td></tr>
 *   <tr><td>{@link IntegrationEventPublisher}</td><td>application/event/publisher</td><td>集成事件出（跨进程）</td></tr>
 *   <tr><td>{@code DomainEventSubscriber}</td><td>application/event/subscriber</td><td>领域事件入（进程内，本角色的对偶）</td></tr>
 * </table>
 *
 * @see com.yoursweakfoe.common.ddd.domain.event.DomainEvent
 * @see IntegrationEventPublisher
 */
public interface DomainEventPublisher {
}
