package com.yoursweakfoe.common.ddd.domain.specification;

import java.util.Objects;

/**
 * 规约（Specification）模式 —— 可组合的领域谓词抽象。
 *
 * <p>把「对象是否满足某条规则」封装成可命名、可复用、可组合的一等对象，
 * 用 {@link #and(Specification)} / {@link #or(Specification)} / {@link #not()} 组合成复杂规则，
 * 比手写嵌套 {@code if} 更可读、可复用。
 *
 * <p>本接口为<strong>纯接口</strong>（零框架依赖，仅 JDK），直接以 lambda 实现：
 * <pre>{@code
 * Specification<Order> sizableOrder = o -> o.getTotalAmount().compareTo(BigDecimal.valueOf(1000)) > 0;
 * Specification<Order> bulkOrder = o -> o.getItems().size() >= 3;
 * }</pre>
 *
 * <h3>定位：可选工具，非强制路径</h3>
 * <p>本接口是领域规则的<strong>可组合表达</strong>抽象，供规则足够复杂、值得命名与复用
 * 的场景使用。两条主路径不经过它：
 * <ul>
 *   <li><b>简单校验</b>：聚合根内显式 {@code if-throw} + {@code BusinessException}（错误码）仍是首选；
 *       Specification 仅在规则需要 {@code and/or/not} 组合时才引入，避免为一条断言造对象。</li>
 *   <li><b>查询过滤</b>：<strong>不经过本接口</strong>——读侧（CQRS）用 MyBatis-Plus
 *       {@code LambdaQueryWrapper} 直接投影 DTO（读侧本就绕过 domain），不把领域谓词引入读路径。</li>
 * </ul>
 *
 * <h3>null 安全约定</h3>
 * <p>组合子产生的规约对 {@code null} 候选对象<strong>统一返回 {@code false}</strong>（视为「不满足」），
 * 短路底层谓词而不调用之，避免 {@code x -> x.getItems()...} 这类实现遇到 null 抛 NPE。
 * 但<strong>裸 lambda 本身</strong>不保证 null 安全——null 短路仅由组合子提供，
 * 直接调用裸规约的 {@code isSatisfiedBy(null)} 仍可能 NPE，由调用方保证候选非 null。
 *
 * <h3>用法示例（校验）</h3>
 * <pre>{@code
 * Specification<Order> positiveAmount = o -> o.getTotalAmount().signum() > 0;
 * Specification<Order> hasItems = o -> !o.getItems().isEmpty();
 *
 * // 复杂规则组合，替代手写嵌套 if
 * if (!positiveAmount.and(hasItems).isSatisfiedBy(order)) {
 *     throw new BusinessException("order:err.invalid");
 * }
 * }</pre>
 *
 * @param <T> 候选对象类型
 */
@FunctionalInterface
public interface Specification<T> {

    /**
     * 判定候选对象是否满足本规约。
     *
     * @param candidate 候选对象（null 时的行为取决于是否为组合子产生的规约，见类型级「null 安全约定」）
     * @return {@code true} 表示满足
     */
    boolean isSatisfiedBy(T candidate);

    /**
     * 与另一规约做逻辑与（AND）——两者都满足才为 {@code true}。
     *
     * @param other 另一条规约
     * @return 组合规约（对 null 候选短路返回 {@code false}）
     */
    default Specification<T> and(Specification<? super T> other) {
        Objects.requireNonNull(other, "other spec must not be null");
        return candidate -> candidate != null
                && isSatisfiedBy(candidate)
                && other.isSatisfiedBy(candidate);
    }

    /**
     * 与另一规约做逻辑或（OR）——任一满足即为 {@code true}。
     *
     * @param other 另一条规约
     * @return 组合规约（对 null 候选短路返回 {@code false}）
     */
    default Specification<T> or(Specification<? super T> other) {
        Objects.requireNonNull(other, "other spec must not be null");
        return candidate -> candidate != null
                && (isSatisfiedBy(candidate) || other.isSatisfiedBy(candidate));
    }

    /**
     * 对本规约取逻辑非（NOT）。
     *
     * @return 取反后的规约（对 null 候选短路返回 {@code false}）
     */
    default Specification<T> not() {
        return candidate -> candidate != null && !isSatisfiedBy(candidate);
    }
}