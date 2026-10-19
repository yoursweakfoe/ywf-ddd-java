package com.yoursweakfoe.common.ddd.domain.model;

/**
 * 实体基类 —— DDD 中所有实体的抽象父类。
 *
 * <p>实体的核心特征是<strong>通过唯一标识（ID）来区分</strong>，而非通过属性值。 两个实体只要 ID 相同且类型相同，即视为同一个实体，即使其他属性不同。
 *
 * <p>本基类提供：
 *
 * <ul>
 *   <li>{@link #entityEquals(Object)} —— 基于 ID 的实体身份判等
 * </ul>
 *
 * <p><strong>关于 ID 字段</strong>：本基类<b>不持有</b> {@code id} 字段， 子类必须自行声明 {@code id} 并通过 Lombok 或手动方式提供
 * {@code getId()} 实现。 这样子类可以自由在 {@code id} 字段上添加所需注解（如 {@code @ExcelIgnore}）。
 *
 * <p><strong>关于乐观锁</strong>：本基类<b>不持有</b> {@code version} 字段。
 * 需要乐观锁的实体在 PO 上声明 {@code version} 字段即可，
 * 版本条件由 UPDATE 语句的手写 XML 文本携带（{@code WHERE ... AND version = #{version}}），无运行时拦截器。
 *
 * <p><strong>version 字段例外规则</strong>：当仓储采用「reconstitute → 行为方法 → toPO → updateById」
 * 路径时，领域对象必须携带 {@code version}（由 reconstitute 回填），
 * 以便 Converter 将其映射到 PO 供 UPDATE 语句的版本条件使用。
 * 这是「领域零持久化关注」原则的<b>已文档化例外</b>：
 * version 在领域层为只读透传（不参与业务决策），仅作为持久化层乐观锁的载体。
 * 若仓储采用「reload PO → 自行定义的合并方法（如 {@code updatePO}）→ updateById」路径，
 * 则领域层无需持有 version（合并方法由 Converter 实现类自行扩充，非框架接口约束）。
 *
 * <p><strong>关于 equals/hashCode</strong>：本基类<b>不覆写</b> {@code equals}/{@code hashCode}。
 * 实体身份由 ID 决定：需要按 ID 判等时请显式调用 {@link #entityEquals(Object)}。
 *
 * <p><strong>Lombok 约定</strong>：领域层实体禁用 {@code @Data}（项目规则 {@code .agents/rules/03}：
 * 仅 {@code @Getter}）——全字段判等对可变实体是反模式（状态变化破坏 Set/Map 语义），
 * 公开 setter 会绕过行为方法的不变量守卫。{@code @Data} 仅用于 PO / DTO / CQE / CO。
 *
 * @param <ID> 标识类型，如 {@link java.util.UUID}、{@link Long} 等
 * @see Identifiable
 * @see AggregateRoot
 */
public abstract class Entity<ID> implements Identifiable<ID> {

    // ==================== 判等方法 ====================

    /**
     * 基于 ID 的实体身份判等（ID 相同且类型相同即为同一实体）。
     *
     * <p>与 Lombok {@code equals}（基于全字段）互不干扰，需要 DDD 身份语义时显式调用本方法。
     */
    public boolean entityEquals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity<?> that = (Entity<?>) o;
        Object thisId = this.getId();
        Object thatId = that.getId();
        if (thisId == null || thatId == null) return false;
        return thisId.equals(thatId);
    }
}
