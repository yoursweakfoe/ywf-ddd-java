package com.yoursweakfoe.common.ddd.application.service;

/**
 * 应用服务标记接口 —— 标识 application 层聚合协调入口（用例门面）。
 *
 * <p>位于 {@code application/{aggregate}/service/}，聚合内唯一，承担全部用例方法：
 * 写用例委托 CommandHandler（返回 DTO → Presenter 呈现为 CO），读用例委托 QueryHandler。
 * 本标记将这类组件显式定型为<strong>应用服务</strong>，与 domain 层跨聚合协调的
 * {@code DomainService} 划清边界：本标记组件编排用例（Handler + Presenter），
 * 不承载领域逻辑；DomainService 承载跨聚合领域逻辑。
 *
 * <p>本接口为<strong>空标记</strong>：价值在「标识应用服务身份」（供架构规则/ArchUnit
 * 识别与约束），而非约束方法签名——每个聚合的用例集合因业务而异，方法签名保持自由。
 *
 * <h3>命名约定：标记用全名，业务类缩写</h3>
 * <p>业务实现类沿用缩写命名 {@code XxxAppService}（{@code App} = Application 的缩写，
 * 仅为类名简洁），与 {@code IntegrationEventPublisher ↔ XxxEventPublisher} 的缩写惯例
 * 同构。标记接口保持语义完整，不受业务类名长度约束。
 *
 * <h3>为什么是空标记，而不是带方法的契约接口？</h3>
 * <ul>
 *   <li>应用服务的用例方法集（{@code placeOrder} / {@code getOrderPage} / ...）是业务专属的，
 *       无统一方法形状可抽——抽成契约方法只会退化成 {@code Object} 参数，丢失类型安全。</li>
 *   <li>标记的价值在于<strong>定型角色 + 提供架构锚点</strong>：例如约束「实现本标记的类
 *       必须位于 application 层」、约束「adapter 入口只能依赖本标记组件（不透传 Handler）」。</li>
 * </ul>
 *
 * <h3>与相关类型的关系</h3>
 * <table>
 *   <tr><th>类型</th><th>层级</th><th>职责</th></tr>
 *   <tr><td>{@code ApplicationService}（本接口）</td><td>application/service</td><td>用例门面：委托 Handler + Presenter</td></tr>
 *   <tr><td>{@code CommandHandler} / {@code QueryHandler}</td><td>application/handler</td><td>单用例执行（与 CQE 1:1）</td></tr>
 *   <tr><td>{@link com.yoursweakfoe.common.ddd.domain.service.DomainService}</td><td>domain/service</td><td>跨聚合领域协调（领域逻辑）</td></tr>
 * </table>
 *
 * @see com.yoursweakfoe.common.ddd.application.handler.command.CommandHandler
 * @see com.yoursweakfoe.common.ddd.application.handler.query.QueryHandler
 * @see com.yoursweakfoe.common.ddd.domain.service.DomainService
 */
public interface ApplicationService {
}
