package com.yoursweakfoe.common.ddd.adapter.event.subscriber;

/**
 * 集成事件订阅者标记接口 —— 标识 adapter 层消费外部服务入站集成事件的入口角色。
 *
 * <p>位于 {@code adapter/event/subscriber/}。实现类为普通 {@code @Component}（MQ 监听注解的
 * 形态随业务所选中间件，标注在方法上），接收外部服务投递的集成事件 → 按契约层载荷类型
 * （{@code IntegrationEvent}）反序列化 → 构建 Command → 透传 ApplicationService 用例方法
 * ——与 REST / 定时任务入口同构，属纯 driving adapter，不落业务判断。
 *
 * <p><strong>边界</strong>：本角色接收<strong>外部</strong>集成事件（跨进程入站）；内部领域事件的
 * 进程内反应由 application 层 {@code DomainEventSubscriber} 承担，本角色的出站对偶是
 * application 层 {@code IntegrationEventPublisher}。消费端幂等（业务键判重）属业务职责，
 * 框架不提供去重设施。
 *
 * <p>本接口为<strong>空标记</strong>：消费方法签名由 MQ 框架的反序列化形态决定（字符串消息体
 * 或载荷对象），消息形状已由契约层 {@code IntegrationEvent} 子类型承载，无统一方法形状可抽。
 * 本框架只提供事件角色词汇、不内建 MQ 对接。
 *
 * <h3>与相关类型的关系</h3>
 * <table>
 *   <tr><th>类型</th><th>层级</th><th>方向</th></tr>
 *   <tr><td>{@code IntegrationEventSubscriber}（本接口）</td><td>adapter/event/subscriber</td><td>集成事件入（MQ 接收）</td></tr>
 *   <tr><td>{@code IntegrationEventPublisher}</td><td>application/event/publisher</td><td>集成事件出（跨进程，本角色的对偶）</td></tr>
 *   <tr><td>{@code DomainEventSubscriber}</td><td>application/event/subscriber</td><td>领域事件入（进程内）</td></tr>
 * </table>
 *
 * @see com.yoursweakfoe.common.contract.dto.event.IntegrationEvent
 * @see com.yoursweakfoe.common.ddd.application.event.publisher.IntegrationEventPublisher
 */
public interface IntegrationEventSubscriber {
}
