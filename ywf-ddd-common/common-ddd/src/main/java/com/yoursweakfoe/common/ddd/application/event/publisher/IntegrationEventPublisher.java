package com.yoursweakfoe.common.ddd.application.event.publisher;

/**
 * 集成事件发布者标记接口 —— 标识 application 层「把领域事实翻译为集成事件并出站」的组件角色。
 *
 * <p>位于 {@code application/{aggregate}/event/publisher/}。职责是把领域内已发生的事实翻译为
 * 集成事件（{@code IntegrationEvent}，载荷类型定义在契约层、随契约 jar 供消费方反序列化），
 * 经业务自持的消息中间件出站。投递的可靠性策略——事务内直发 / 本地消息表 / MQ 事务消息 /
 * 对账补偿——由业务按一致性要求决定，本角色的身份与包位置不随之漂移。
 *
 * <p><strong>边界</strong>：本角色仅负责集成事件的<strong>跨进程</strong>出站；领域事件的进程内
 * 通知由同包 {@link DomainEventPublisher} 承担。入站对偶见 adapter 层
 * {@code IntegrationEventSubscriber}。
 *
 * <p>本接口为<strong>空标记</strong>：出站方法形状（单事件 / 批量、同步 / 异步、带不带消息头）
 * 由所选中间件决定，无统一形状可抽。标记的价值在角色定型：实现类集中于此包、实现本标记，
 * 「谁在代表本服务对外发声」一目了然。
 *
 * <h3>与相关类型的关系</h3>
 * <table>
 *   <tr><th>类型</th><th>层级</th><th>方向</th></tr>
 *   <tr><td>{@code IntegrationEventPublisher}（本接口）</td><td>application/event/publisher</td><td>集成事件出（跨进程）</td></tr>
 *   <tr><td>{@link DomainEventPublisher}</td><td>application/event/publisher</td><td>领域事件出（进程内）</td></tr>
 *   <tr><td>{@code IntegrationEventSubscriber}</td><td>adapter/event/consumer</td><td>集成事件入（MQ，本角色的对偶）</td></tr>
 * </table>
 *
 * @see com.yoursweakfoe.common.contract.dto.event.IntegrationEvent
 * @see DomainEventPublisher
 */
public interface IntegrationEventPublisher {
}
