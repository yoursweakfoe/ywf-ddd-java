package com.yoursweakfoe.common.ddd.infrastructure.converter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基础转换器接口 —— 基础设施层 Domain 与 PO 之间的双向转换契约。
 *
 * <p>与 {@link com.yoursweakfoe.common.ddd.application.assembler.BasicAssembler} 对称：
 *
 * <ul>
 *   <li>BasicAssembler —— 应用层：DTO ↔ Domain
 *   <li>BasicConverter —— 基础设施层：Domain ↔ PO
 * </ul>
 *
 * <h3>实现约定（纯手写显式映射）</h3>
 * <ul>
 *   <li>实现类为普通 {@code @Component} 类，逐字段显式映射，不使用代码生成器
 *   <li>{@code toDomain} —— 通过聚合根的 {@code reconstitute()} 静态工厂重建，不经过业务校验/事件注册
 *   <li>{@code toPO} —— 提取领域对象当前状态的快照，不修改 Domain 任何状态
 *   <li>Domain 的状态变更只通过其行为方法（如 {@code place()}、{@code cancel()}），内聚校验 + 事件注册
 *   <li>持久化前的不变量校验由 {@code MybatisPersistence} 自动调用 {@code validate()}，Converter 不管校验
 *   <li>字段增删时必须同步修改映射方法，并以往返测试守护完整性
 * </ul>
 *
 * <p>List/Set 方法已提供 default 实现（委托单体方法），实现类只需关注
 * {@code toDomain}/{@code toPO} 核心逻辑。
 *
 * <p>乐观锁说明：需要乐观锁的实体在 PO 上声明 {@code @Version} 字段，
 * 由 {@code OptimisticLockerInnerInterceptor} 自动处理，领域层无需感知。
 *
 * @param <Domain> 领域实体类型
 * @param <PO> 持久化对象类型
 */
public interface BasicConverter<Domain, PO> {

    // ==================== 单体转换 ====================

    /**
     * 将 PO 转换为领域对象
     *
     * @param po 持久化对象
     * @return 对应的领域对象
     */
    Domain toDomain(PO po);

    /**
     * 将领域对象转换为 PO
     *
     * @param domain 领域对象
     * @return 对应的 PO
     */
    PO toPO(Domain domain);

    // ==================== 增量更新 ====================

    /**
     * 将 PO 中的属性合并到已有领域对象上（不创建新实例）。
     *
     * <p>富领域模型通常抛出 {@code UnsupportedOperationException}，由 reconstitute 替代。
     *
     * @param po 持久化对象
     * @param domain 待更新的领域对象
     */
    void updateDomain(PO po, Domain domain);

    /**
     * 将领域对象中的属性合并到已有 PO 上（不创建新实例）。
     *
     * @param domain 领域对象
     * @param po 待更新的持久化对象
     */
    void updatePO(Domain domain, PO po);

    // ==================== List 转换（default 委托单体方法） ====================

    /**
     * 批量 PO → Domain
     *
     * @param poList PO 列表
     * @return 对应的领域对象列表
     */
    default List<Domain> toDomainList(List<PO> poList) {
        return poList.stream().map(this::toDomain).toList();
    }

    /**
     * 批量 Domain → PO
     *
     * @param domainList 领域对象列表
     * @return 对应的 PO 列表
     */
    default List<PO> toPOList(List<Domain> domainList) {
        return domainList.stream().map(this::toPO).toList();
    }

    // ==================== Set 转换（default 委托单体方法） ====================

    /**
     * 批量 PO → Domain（去重集合，适用于一对多关联子实体集合）
     *
     * @param poSet PO 集合
     * @return 对应的领域对象集合
     */
    default Set<Domain> toDomainSet(Set<PO> poSet) {
        return poSet.stream().map(this::toDomain).collect(Collectors.toSet());
    }

    /**
     * 批量 Domain → PO（去重集合）
     *
     * @param domainSet 领域对象集合
     * @return 对应的 PO 集合
     */
    default Set<PO> toPOSet(Set<Domain> domainSet) {
        return domainSet.stream().map(this::toPO).collect(Collectors.toSet());
    }
}
