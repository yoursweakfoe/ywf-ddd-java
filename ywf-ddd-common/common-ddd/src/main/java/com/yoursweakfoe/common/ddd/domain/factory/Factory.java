package com.yoursweakfoe.common.ddd.domain.factory;

/**
 * 领域工厂标记接口 —— 标识创建复杂领域对象的工厂。
 *
 * <p>当聚合根或实体的构造过程涉及复杂校验、多步组装或需要协调多个值对象时，
 * 应将创建逻辑封装到实现本接口的工厂类中，而非散落在应用层或构造器中。
 *
 * <h3>使用原则（按需）</h3>
 * <ul>
 *   <li>简单聚合：直接用构造器 / 静态工厂方法（{@code new Order(...)} 或 {@code Order.reconstitute(...)}）
 *   <li>复杂聚合：抽取独立 Factory 类，实现本接口，内部定义一个或多个 {@code create(...)} 方法
 * </ul>
 *
 * <h3>设计约束</h3>
 * <ul>
 *   <li>本接口为纯标记，不强制方法签名 —— 工厂入参因业务而异，无法统一
 *   <li>工厂属于 Domain 层，入参只能是领域层概念（值对象、基本类型、领域服务），禁止感知 Command/DTO
 *   <li>工厂保证聚合一致性 —— 返回前确保聚合根处于合法状态
 * </ul>
 *
 * <h3>使用示例</h3>
 *
 * <pre>{@code
 * // 单入参（值对象）
 * @Component
 * public class OrderFactory implements Factory {
 *     public Order create(List<OrderItem> items, CustomerId customerId) {
 *         // 复杂校验 + 多步组装
 *         return new Order(UUID.randomUUID(), items, customerId);
 *     }
 * }
 *
 * // 多 create 方法（不同创建场景）
 * @Component
 * public class PaymentFactory implements Factory {
 *     public Payment createFromOrder(Order order, PaymentMethod method) { ... }
 *     public Payment createRefund(Payment original, BigDecimal amount) { ... }
 * }
 * }</pre>
 *
 * @see com.yoursweakfoe.common.ddd.domain.model.AggregateRoot
 * @see com.yoursweakfoe.common.ddd.domain.service.DomainService
 */
public interface Factory {
}
