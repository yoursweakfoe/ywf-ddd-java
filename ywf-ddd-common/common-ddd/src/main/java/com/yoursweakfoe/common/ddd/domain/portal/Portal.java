package com.yoursweakfoe.common.ddd.domain.portal;

/**
 * 外部资源访问标记接口 —— 标识 Domain 层对外部系统的抽象依赖（传送门）。
 *
 * <p>当领域逻辑需要调用外部能力（支付、文件存储、短信、RPC、ES 等）时，
 * 在 Domain 层定义以 {@code Portal} 结尾的接口继承本标记，
 * 由 Infrastructure 层的 Gateway 实现（以 {@code Gateway} 结尾）。
 *
 * <h3>命名对偶</h3>
 * <table>
 *   <tr><th>层</th><th>命名</th><th>示例</th></tr>
 *   <tr><td>Domain（接口）</td><td>{@code XxxPortal}</td><td>{@code PaymentPortal}、{@code StoragePortal}</td></tr>
 *   <tr><td>Infrastructure（实现）</td><td>{@code XxxGateway}</td><td>{@code AlipayPaymentGateway}、{@code AliOssStorageGateway}</td></tr>
 * </table>
 *
 * <h3>与 Repository 的对偶关系</h3>
 * <ul>
 *   <li>Repository —— "存取我的世界"（聚合的持久化，实现在 infrastructure/persistence）
 *   <li>Portal —— "打开传送门，获取外部能力"（外部资源访问，实现在 infrastructure/gateway）
 * </ul>
 *
 * <h3>使用示例</h3>
 *
 * <pre>{@code
 * // Domain 层接口
 * public interface PaymentPortal extends Portal {
 *     PaymentResult pay(UUID orderId, BigDecimal amount, String currency);
 * }
 *
 * // Infrastructure 层实现（含 ACL 翻译）
 * @Component
 * public class AlipayPaymentGateway implements PaymentPortal {
 *     private final AlipayClient alipayClient;
 *
 *     @Override
 *     public PaymentResult pay(UUID orderId, BigDecimal amount, String currency) {
 *         AlipayTradePayResponse resp = alipayClient.execute(buildRequest(orderId, amount, currency));
 *         return new PaymentResult(resp.getTradeNo(), "10000".equals(resp.getCode()), new BigDecimal(resp.getTotalAmount()));
 *     }
 * }
 * }</pre>
 *
 * <h3>什么属于 Portal / Gateway</h3>
 * <ul>
 *   <li>领域逻辑依赖的外部能力（支付、汇率查询、文件存储、短信通知）
 *   <li>每个实现包含：技术调用 + ACL 模型翻译 + 容错（超时/降级/重试）
 * </ul>
 *
 * <h3>什么不属于 Portal / Gateway</h3>
 * <ul>
 *   <li>应用层编排型 RPC（如"下单后通知物流服务"）→ 应用层直接调用
 *   <li>查询组装型调用（如"查用户信息拼 DTO"）→ 应用层 Handler 内完成
 *   <li>Repository 实现 → infrastructure/persistence
 * </ul>
 *
 * @see com.yoursweakfoe.common.ddd.domain.repository.Repository
 */
public interface Portal {
}
