package com.yoursweakfoe.common.ddd.domain.model;

/**
 * 值对象标记接口 —— DDD 中值对象的语义标记。
 *
 * <p>值对象的核心特征是<strong>没有唯一标识</strong>，完全通过其属性值来定义。
 * 两个值对象只要所有属性值相同，即视为相等。值对象应当是<strong>不可变的</strong>。
 *
 * <p>
 *
 * <h3>推荐实现方式</h3>
 *
 * <ul>
 *   <li><strong>首选</strong>：使用 Java {@code record} —— 天然不可变，自动生成 equals/hashCode/toString，最简洁优雅
 *   <li><strong>备选</strong>：普通类 + Lombok {@code @Value} —— 编译期生成全字段 equals/hashCode，所有字段 final
 * </ul>
 *
 * <p>
 *
 * <h3>使用示例（record 方式）</h3>
 *
 * <pre>{@code
 * public record Money(BigDecimal amount, String currency) implements ValueObject {
 *     public Money {
 *         if (amount == null) throw new BusinessException("common:err.amountRequired");
 *         if (currency == null) throw new BusinessException("common:err.currencyRequired");
 *     }
 * }
 * }</pre>
 *
 * <p>
 *
 * <h3>设计原则</h3>
 *
 * <ul>
 *   <li>不可变性（Immutability）：创建后不可修改
 *   <li>自包含校验：在构造时完成业务规则校验
 *   <li>侧重行为：可以包含业务计算方法（如 Money.add()）
 *   <li>可替换性：整体替换而非部分修改
 * </ul>
 *
 * @see Entity
 * @see AggregateRoot
 */
public interface ValueObject {}
