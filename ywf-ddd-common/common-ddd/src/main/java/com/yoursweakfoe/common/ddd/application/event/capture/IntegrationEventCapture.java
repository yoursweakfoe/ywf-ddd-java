package com.yoursweakfoe.common.ddd.application.event.capture;

/**
 * 集成事件出站捕获器标记接口 —— 标识 application 层将领域事件翻译为契约集成事件并同事务捕获入集成 Outbox 的 Capture。
 *
 * <p>位于 {@code application/{aggregate}/event/capture/}，被 CommandHandler 或
 * {@code DomainEventListener} 显式调用，将领域事件（丰富领域细节）翻译为契约
 * IntegrationEvent（仅外部需要的精简字段）后经集成 Outbox 与调用方事务<strong>同事务入箱</strong>。
 * 本标记将这类组件显式定型为<strong>集成事件出站捕获</strong>，与 domain 层进程内派发的
 * {@code DomainEventPublisher} 划清边界：本标记组件消费领域事件、产出 IntegrationEvent 并入箱；
 * 后者仅负责<strong>进程内</strong>领域事件派发（由框架领域排空器在排空事务内调用，业务侧不直接调用）。
 *
 * <p><strong>本接口组件从不投递</strong>：跨服务出站投递由框架集成排空器经
 * {@code IntegrationEventSender} 完成（messageId = outbox 行 id）——入箱与投递之间的
 * dual-write 窗口由 Outbox 排空机制关闭（入箱与「领域行标记完成」原子提交，投递失败退避重投）。
 *
 * <p>本接口为<strong>空标记</strong>：价值在「标识出站捕获器身份」（供架构规则/ArchUnit
 * 识别与约束），而非约束方法签名——每个聚合的翻译目标 IntegrationEvent 因业务而异，
 * 方法签名保持自由。
 *
 * <h3>为什么是空标记，而不是带方法的契约接口？</h3>
 * <ul>
 *   <li>翻译目标由 contract 层 {@code IntegrationEvent} 子类型承载，方法签名形状
 *       （{@code captureXxx(XxxDomainEvent)}）是业务专属的，抽成契约方法无统一形状可表达。</li>
 *   <li>标记的价值在于<strong>定型角色 + 提供架构锚点</strong>：例如约束「AppService 不直接
 *       依赖 capture」（捕获只经 CommandHandler / DomainEventListener 调用），以及约束
 *       出站捕获器必须位于 application 层。</li>
 * </ul>
 *
 * <h3>与相关类型的关系</h3>
 * <table>
 *   <tr><th>类型</th><th>层级</th><th>方向</th></tr>
 *   <tr><td>{@link com.yoursweakfoe.common.ddd.application.event.listener.DomainEventListener}</td><td>application/event/listener</td><td>内部领域事件入（消费）</td></tr>
 *   <tr><td>{@code IntegrationEventCapture}（本接口）</td><td>application/event/capture</td><td>集成事件出（翻译 + 同事务入箱）</td></tr>
 *   <tr><td>{@code IntegrationEvent}（common-contract）</td><td>contract/dto/event/integration</td><td>跨服务契约（本接口的实现类翻译并捕获之）</td></tr>
 *   <tr><td>adapter Consumer</td><td>adapter/event/consumer</td><td>集成事件入（MQ）</td></tr>
 * </table>
 *
 * @see com.yoursweakfoe.common.contract.dto.event.integration.IntegrationEvent
 * @see com.yoursweakfoe.common.ddd.application.event.listener.DomainEventListener
 * @see com.yoursweakfoe.common.ddd.domain.event.publisher.DomainEventPublisher
 */
public interface IntegrationEventCapture {
}
