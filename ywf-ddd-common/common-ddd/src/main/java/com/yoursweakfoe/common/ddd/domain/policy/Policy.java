package com.yoursweakfoe.common.ddd.domain.policy;

/**
 * 领域策略标记接口 —— 标识一条可插拔的领域规则（Strategy 模式在 DDD 领域层的应用）。
 *
 * <p>Policy 封装<strong>纯计算/决策逻辑</strong>，无状态、无副作用。
 * 由 Domain Service 收集结果后操作实体，Policy 本身不修改任何对象。
 *
 * <h3>核心契约</h3>
 * <ul>
 *   <li>{@link #isApplicable(Object)} —— 判断本策略是否适用于当前上下文
 *   <li>业务方法（如 {@code calculateDiscount(Order)}）—— 由子接口按业务场景定义
 * </ul>
 *
 * <h3>三种组合形态</h3>
 * <table>
 *   <tr><th>形态</th><th>顺序要求</th><th>主流程逻辑</th><th>典型场景</th></tr>
 *   <tr><td>互斥型</td><td>严格（{@code @Order}）</td><td>命中第一个即返回</td><td>折扣计算、风控拦截</td></tr>
 *   <tr><td>叠加型</td><td>无关</td><td>遍历累加</td><td>运费减免、优惠叠加</td></tr>
 *   <tr><td>精准路由型</td><td>无关</td><td>Map.get(type)</td><td>多租户策略、支付渠道路由</td></tr>
 * </table>
 *
 * <h3>使用示例</h3>
 *
 * <pre>{@code
 * // 1. 定义业务子接口（绑定上下文类型）
 * public interface DiscountPolicy extends Policy<Order> {
 *     BigDecimal calculateDiscount(Order order);
 * }
 *
 * // 2. 实现具体策略（无状态单例，无需 instanceof）
 * @Component
 * @Order(1)
 * public class VipDiscountPolicy implements DiscountPolicy {
 *     @Override
 *     public boolean isApplicable(Order order) {
 *         return order.getUser().isVip();
 *     }
 *
 *     @Override
 *     public BigDecimal calculateDiscount(Order order) {
 *         return new BigDecimal("0.80");
 *     }
 * }
 *
 * // 3. Domain Service 编排（互斥型）
 * @Service
 * public class OrderPricingDomainService implements DomainService {
 *     private final List<DiscountPolicy> policies;  // Spring 自动收集，@Order 排序
 *
 *     public BigDecimal calculateFinalDiscount(Order order) {
 *         for (DiscountPolicy policy : policies) {
 *             if (policy.isApplicable(order)) {
 *                 return policy.calculateDiscount(order);
 *             }
 *         }
 *         return BigDecimal.ONE;
 *     }
 * }
 * }</pre>
 *
 * <h3>与 Domain Service 的区别</h3>
 * <ul>
 *   <li>Policy —— 纯计算/决策，无副作用，不修改任何对象
 *   <li>Domain Service —— 编排操作，可修改实体状态，可调用 Repository
 * </ul>
 *
 * @param <C> 决策上下文类型，通常为领域实体或聚合根（如 {@code Order}）
 * @see com.yoursweakfoe.common.ddd.domain.service.DomainService
 */
public interface Policy<C> {

    /**
     * 判断本策略是否适用于当前上下文。
     *
     * @param context 决策上下文（领域对象）
     * @return {@code true} 表示本策略适用，主流程将继续调用业务方法
     */
    boolean isApplicable(C context);
}
