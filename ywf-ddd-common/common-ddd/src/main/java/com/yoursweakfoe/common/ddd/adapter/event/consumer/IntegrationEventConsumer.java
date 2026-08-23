package com.yoursweakfoe.common.ddd.adapter.event.consumer;

/**
 * 集成事件入站消费者标记接口 —— 标识 adapter 层消费 MQ 集成事件（IntegrationEvent）的入口。
 *
 * <p>位于 {@code adapter/event/consumer/}，实现类为普通 {@code @Component}（MQ 监听注解如
 * {@code @RocketMQMessageListener} 标注在方法上），接收外部服务投递的集成事件 → 反序列化 →
 * 构建 Command → 透传 ApplicationService 用例方法（与 REST 入口同构，纯入口）。本标记将这类
 * 组件显式定型为<strong>集成事件入站</strong>，与 application 层出站
 * {@code IntegrationEventPublisher} 划清边界：本标记组件接收<strong>外部</strong>集成事件（MQ），
 * 后者翻译领域事件为集成事件并<strong>投递</strong> MQ。
 *
 * <p>本接口为<strong>空标记</strong>：价值在「标识入站消费者身份」（供架构规则/ArchUnit 识别），
 * 而非约束方法签名——消费方法签名（{@code onXxx(String messageBody)} / {@code onXxx(IntegrationEvent)}）
 * 由 MQ 框架的反序列化形态决定，且消息形状已由 contract 层的 {@code IntegrationEvent} 子类型承载，
 * 无统一方法形状可抽。
 *
 * <h3>与相关类型的关系</h3>
 * <table>
 *   <tr><th>类型</th><th>层级</th><th>方向</th></tr>
 *   <tr><td>{@code IntegrationEventConsumer}（本接口）</td><td>adapter/event/consumer</td><td>集成事件入（MQ 接收）</td></tr>
 *   <tr><td>{@link com.yoursweakfoe.common.ddd.application.event.publisher.IntegrationEventPublisher}</td><td>application/event/publisher</td><td>集成事件出（MQ 投递）</td></tr>
 *   <tr><td>{@link com.yoursweakfoe.common.ddd.application.event.listener.DomainEventListener}</td><td>application/event/listener</td><td>内部领域事件入（进程内）</td></tr>
 *   <tr><td>{@code IntegrationEvent}（common-contract）</td><td>contract/dto/event/integration</td><td>跨服务契约（本接口实现类反序列化之）</td></tr>
 * </table>
 *
 * <p><strong>落地状态</strong>：本标记为框架预留（依赖 common-mq 模块，该模块尚未建设，
 * 当前无实现类）。实现模板见 {@code docs/application/cookbook/mq-consumer.md}——待 common-mq
 * 落地后按模板补全 {@code adapter/event/consumer/} 下实现类并实现本标记。
 *
 * @see com.yoursweakfoe.common.contract.dto.event.integration.IntegrationEvent
 * @see com.yoursweakfoe.common.ddd.application.event.publisher.IntegrationEventPublisher
 * @see com.yoursweakfoe.common.ddd.application.event.listener.DomainEventListener
 */
public interface IntegrationEventConsumer {
}
