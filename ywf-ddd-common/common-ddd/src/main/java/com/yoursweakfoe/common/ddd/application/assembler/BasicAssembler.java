package com.yoursweakfoe.common.ddd.application.assembler;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基础装配器接口 —— 应用层 DTO 与领域对象之间的双向转换契约。
 *
 * <p>在 DDD 分层架构中，应用层需要将外部传入的 DTO 转换为领域对象，
 * 或将领域对象转换为 DTO 返回给调用方。本接口定义了这一转换的标准方法集。
 *
 * <h3>实现约定（纯手写显式映射）</h3>
 * <ul>
 *   <li>实现类为普通 {@code @Component} 类，逐字段显式映射，不使用代码生成器
 *   <li>富领域模型（聚合根无 setter）：{@code toDomain}/{@code updateDomain}/{@code updateDTO}
 *       通常抛 {@code UnsupportedOperationException}，重建走 {@code reconstitute()}
 *   <li>字段增删时必须同步修改映射方法，并以往返测试守护完整性
 * </ul>
 *
 * <p>List/Set 方法已提供 default 实现（委托单体方法），实现类只需关注
 * {@code toDomain}/{@code toDTO} 核心逻辑。
 *
 * @param <Domain> 领域对象类型
 * @param <DTO> 数据传输对象类型
 */
public interface BasicAssembler<Domain, DTO> {

    // ==================== 单体转换 ====================

    /**
     * 将 DTO 转换为领域对象
     *
     * @param dto DTO 对象
     * @return 对应的领域对象
     */
    Domain toDomain(DTO dto);

    /**
     * 将领域对象转换为 DTO
     *
     * @param domain 领域对象
     * @return 对应的 DTO
     */
    DTO toDTO(Domain domain);

    // ==================== 增量更新 ====================

    /**
     * 将 DTO 中的属性合并到已有领域对象上（不创建新实例）。
     *
     * <p>富领域模型（聚合根无 setter）通常不支持增量更新，保持默认抛
     * {@link UnsupportedOperationException}（重建走 {@code reconstitute()}）。
     * 贫血模型需要时覆写本方法，参考实现思路：
     * <pre>{@code
     * @Override
     * public void updateDomain(XxxDTO dto, XxxDomain domain) {
     *     domain.setName(dto.getName());
     *     domain.setQuantity(dto.getQuantity());
     *     // ... 逐字段显式赋值
     * }
     * }</pre>
     *
     * @param dto DTO 对象
     * @param domain 待更新的领域对象
     */
    default void updateDomain(DTO dto, Domain domain) {
        throw new UnsupportedOperationException(
                "updateDomain not supported for rich domain model; use reconstitute() instead");
    }

    /**
     * 将领域对象中的属性合并到已有 DTO 上（不创建新实例）。
     *
     * <p>默认抛 {@link UnsupportedOperationException}，需要时覆写：
     * <pre>{@code
     * @Override
     * public void updateDTO(XxxDomain domain, XxxDTO dto) {
     *     dto.setName(domain.getName());
     *     // ... 逐字段显式赋值
     * }
     * }</pre>
     *
     * @param domain 领域对象
     * @param dto 待更新的 DTO
     */
    default void updateDTO(Domain domain, DTO dto) {
        throw new UnsupportedOperationException(
                "updateDTO not supported for rich domain model");
    }

    // ==================== List 转换（default 委托单体方法） ====================

    /**
     * 批量 DTO → Domain
     *
     * @param dtoList DTO 列表
     * @return 对应的领域对象列表
     */
    default List<Domain> toDomainList(List<DTO> dtoList) {
        return dtoList.stream().map(this::toDomain).toList();
    }

    /**
     * 批量 Domain → DTO
     *
     * @param domainList 领域对象列表
     * @return 对应的 DTO 列表
     */
    default List<DTO> toDTOList(List<Domain> domainList) {
        return domainList.stream().map(this::toDTO).toList();
    }

    // ==================== Set 转换（default 委托单体方法） ====================

    /**
     * 批量 DTO → Domain（去重集合）
     *
     * @param dtoSet DTO 集合
     * @return 对应的领域对象集合
     */
    default Set<Domain> toDomainSet(Set<DTO> dtoSet) {
        return dtoSet.stream().map(this::toDomain).collect(Collectors.toSet());
    }

    /**
     * 批量 Domain → DTO（去重集合）
     *
     * @param domainSet 领域对象集合
     * @return 对应的 DTO 集合
     */
    default Set<DTO> toDTOSet(Set<Domain> domainSet) {
        return domainSet.stream().map(this::toDTO).collect(Collectors.toSet());
    }
}
