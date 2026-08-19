package com.yoursweakfoe.common.ddd.application.assembler;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基础装配器接口 —— 应用层 DTO 与领域对象之间的转换契约。
 *
 * <p>在 DDD 分层架构中，应用层需要将领域对象转换为 DTO 返回（写路径 Handler 的出口）。
 * 本接口是<strong>最小契约</strong>：只声明方向恒定的转换方法与批量委托组合子，
 * <strong>不携带任何与模型形态相关的非通用方法</strong>（如增量更新 updateXxx）——
 * 抽取层代码最小化，子类自行扩充。
 *
 * <h3>实现约定（纯手写显式映射）</h3>
 * <ul>
 *   <li>实现类为普通 {@code @Component} 类，逐字段显式映射，不使用代码生成器
 *   <li>{@code toDomain} —— 贫血模型按需实现；富领域模型（聚合根无 setter）不支持
 *       DTO → Domain 方向时，可在实现类抛 {@code UnsupportedOperationException}，
 *       重建走聚合根的 {@code reconstitute()} 静态工厂
 *   <li>字段增删时必须同步修改映射方法，并以往返测试守护完整性
 * </ul>
 *
 * <p>List/Set 方法已提供 default 实现（委托单体方法），实现类只需关注
 * {@code toDomain}/{@code toDTO} 核心逻辑。
 *
 * <p><strong>增量更新说明</strong>：本接口<strong>不定义</strong> {@code updateDomain} /
 * {@code updateDTO}。需要增量合并的实现类（如贫血模型）自行在类上声明普通方法
 * （{@code public void mergeInto(XxxDTO dto, XxxDomain domain)}），
 * 富领域模型天然无需任何"不支持就要写 throw"的样板。
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