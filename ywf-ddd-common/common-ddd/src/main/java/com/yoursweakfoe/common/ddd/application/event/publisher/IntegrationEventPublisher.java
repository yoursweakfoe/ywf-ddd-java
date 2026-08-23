package com.yoursweakfoe.common.ddd.application.event.publisher;

import com.yoursweakfoe.common.ddd.domain.event.publisher.DomainEventPublisher;

/**
 * 集成事件出站发布器标记接口 —— 标识 application 层将领域事件翻译为契约集成事件并投递 MQ 的 Publisher。
 *
 * <p>位于 {@code application/{aggregate}/event/publisher/}，被 CommandHandler 或
 * {@link DomainEventListener} 显式调用，将领域事件（丰富领域细节）翻译为契约
 * IntegrationEvent（仅外部需要的精简字段）后投递到 MQ。本标记将这类组件显式定型为
 * <strong>集成事件出站</strong>，与 domain 层进程内发布的 {@code DomainEventPublisher}
 * 划清边界：本标记组件消费领域事件、产出 IntegrationEvent 并<strong>跨服务</strong>投递；
 * 后者仅负责<strong>进程内</strong>领域事件发布（仓储持久化后自动触发，业务侧不直接调用）。
 *
 * <p>本接口为<strong>空标记</strong>：价值在「标识出站 Publisher 身份」（供架构规则/ArchUnit
 * 识别与约束），而非约束方法签名——每个聚合的翻译目标 IntegrationEvent 因业务而异，
 * 方法签名保持自由。
 *
 * <h3>为什么是空标记，而不是带方法的契约接口？</h3>
 * <ul>
 *   <li>翻译目标由 contract 层 {@code IntegrationEvent} 子类型承载，方法签名形状
 *       （{@code publishXxx(XxxDomainEvent)}）是业务专属的，抽成契约方法无统一形状可表达。</li>
 *   <li>标记的价值在于<strong>定型角色 + 提供架构锚点</strong>：例如约束「AppService 不直接
 *       依赖 publisher」（发布只经 CommandHandler / DomainEventListener），以及约束
 *       出站 Publisher 必须位于 application 层。</li>
 * </ul>
 *
 * <h3>与相关类型的关系</h3>
 * <table>
 *   <tr><th>类型</th><th>层级</th><th>方向</th></tr>
 *   <tr><td>{@link com.yoursweakfoe.common.ddd.application.event.listener.DomainEventListener}</td><td>application/event/listener</td><td>内部领域事件入（消费）</td></tr>
 *   <tr><td>{@code IntegrationEventPublisher}（本接口）</td><td>application/event/publisher</td><td>集成事件出（投递）</td></tr>
 *   <tr><td>{@code IntegrationEvent}（common-contract）</td><td>contract/dto/event/integration</td><td>跨服务契约（本接口的实现类翻译并投递之）</td></tr>
 *   <tr><td>adapter Consumer</td><td>adapter/event/consumer</td><td>集成事件入（MQ）</td></tr>
 * </table>
 *
 * @see com.yoursweakfoe.common.contract.dto.event.integration.IntegrationEvent
 * @see com.yoursweakfoe.common.ddd.application.event.listener.DomainEventListener
 * @see DomainEventPublisher
 */
public interface IntegrationEventPublisher {
}
