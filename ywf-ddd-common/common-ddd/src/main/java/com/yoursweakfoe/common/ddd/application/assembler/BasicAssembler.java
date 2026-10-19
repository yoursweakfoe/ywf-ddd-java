package com.yoursweakfoe.common.ddd.application.assembler;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基础装配器接口 —— 应用层 Domain → DTO 的<strong>单向</strong>转换契约。
 *
 * <h3>为什么只有单向（教义定档，全仓唯一出处）</h3>
 *
 * <p>聚合的构造入口恒定只有两扇门：<strong>Factory</strong>（从无到有的新建）与
 * <strong>{@code reconstitute()}</strong>（存储重建），DTO 只是只读的出口视图——
 * 它出现的目的永远是「离开应用层」，而不是「进入聚合」。确有跨用例传递意图者，
 * 构造 <strong>Command</strong>；需要聚合当前状态者，<strong>重新 load</strong>。
 * {@code version} 字段的存在是为跨进程回显后做 compare-and-set，不是进程内免查询的
 * 后门：回放旧版本号的写入对自身撞乐观锁，语义当场即破。确有「从存档数据重建聚合」
 * 需求者（草稿、快照等业务场景），在聚合上开<strong>专属工厂方法</strong>并把输入
 * 当作不可信数据走完整校验，该入口不回流至装配契约。贫血模型的输入补丁同理走
 * Command + 实体行为方法，或按 {@code mergeInto} 先例（见下）留在实现类。
 *
 * <p>因此本接口<strong>不声明</strong>任何 DTO → Domain 方法（曾声明 {@code toDomain}
 * 及其批量默认——死方向，全部实现者被迫写 throw 样板，已连默认乘数一并切除；
 * 回潮守卫见 {@code BasicAssemblerPresenterTest} 的方向反射断言）。
 *
 * <h3>最小契约</h3>
 *
 * <p>只声明方向恒定的转换方法与批量委托组合子，<strong>不携带任何与模型形态相关的
 * 非通用方法</strong>（如增量更新 updateXxx）——抽取层代码最小化，子类自行扩充：
 * 需要增量合并的实现类（如贫血模型）自行在类上声明普通方法
 * （{@code public void mergeInto(XxxDTO dto, XxxDomain domain)}），接口保持中立。
 *
 * <h3>实现约定（纯手写显式映射）</h3>
 * <ul>
 *   <li>实现类为普通 {@code @Component} 类，逐字段显式映射，不使用代码生成器
 *   <li>字段增删时必须同步修改映射方法，并以对字段逐一断言的测试守护完整性
 *       （读侧 DTO 不经本契约：读路径由 QueryRepository 从 PO 直接投影）
 * </ul>
 *
 * <p>List/Set 方法已提供 default 实现（委托 {@code toDTO} 单体方法），
 * 实现类只需关注 {@code toDTO} 核心逻辑。
 *
 * @param <Domain> 领域对象类型
 * @param <DTO> 数据传输对象类型
 */
public interface BasicAssembler<Domain, DTO> {

    // ==================== 单体转换 ====================

    /**
     * 将领域对象转换为 DTO（本契约唯一方向，语义见类 javadoc 教义段）
     *
     * @param domain 领域对象
     * @return 对应的 DTO
     */
    DTO toDTO(Domain domain);

    // ==================== List 转换（default 委托单体方法） ====================

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
     * 批量 Domain → DTO（去重集合）
     *
     * @param domainSet 领域对象集合
     * @return 对应的 DTO 集合
     */
    default Set<DTO> toDTOSet(Set<Domain> domainSet) {
        return domainSet.stream().map(this::toDTO).collect(Collectors.toSet());
    }
}
