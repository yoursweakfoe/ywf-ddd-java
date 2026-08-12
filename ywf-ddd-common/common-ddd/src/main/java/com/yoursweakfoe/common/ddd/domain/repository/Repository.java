package com.yoursweakfoe.common.ddd.domain.repository;

import com.yoursweakfoe.common.ddd.domain.model.Identifiable;
import java.util.Optional;

/**
 * 领域层仓储顶层接口 —— 领域实体持久化的稳定契约。
 *
 * <p>本接口定义在领域层（零基础设施依赖），是业务代码与持久化之间的唯一抽象边界。
 * 管理任何实现了 {@link Identifiable} 的领域对象；若对象同时是
 * {@link com.yoursweakfoe.common.ddd.domain.model.AggregateRoot}，
 * 则实现层会自动附加不变量校验和领域事件发布。
 *
 * <p>业务 Repository 接口应继承本接口，业务 Repository 实现类通过组合
 * {@code MybatisRepositorySupport} 获得具体能力，而非继承框架实现类。
 *
 * <h3>设计原则</h3>
 * <ul>
 *   <li>本接口签名遵循语义版本（SemVer），major 版本才允许破坏性变更
 *   <li>新增能力通过 default 方法或扩展接口提供，不修改已有方法签名
 *   <li>业务代码只依赖本接口，不依赖 MyBatis/数据库相关实现类
 * </ul>
 *
 * @param <Domain> 领域对象类型（必须实现 {@link Identifiable}）
 * @param <ID>     标识类型
 */
public interface Repository<Domain extends Identifiable<ID>, ID> {

    /**
     * 根据 ID 查找领域对象。
     *
     * @param id 唯一标识
     * @return 领域对象实例，不存在时返回 empty
     */
    Optional<Domain> findById(ID id);

    /**
     * 保存新领域对象（INSERT）。
     *
     * <p>实现应保证：若为聚合根，持久化前自动调用 {@code validate()}，持久化后发布领域事件。
     *
     * @param domain 领域对象实例
     */
    void save(Domain domain);

    /**
     * 更新已有领域对象（UPDATE）。
     *
     * <p>实现应保证：若为聚合根，持久化前自动调用 {@code validate()}，乐观锁冲突时抛出异常。
     *
     * @param domain 领域对象实例
     */
    void update(Domain domain);

    /**
     * 判断指定 ID 的领域实体是否存在。
     *
     * @param id 唯一标识
     * @return {@code true} 表示存在
     */
    boolean exists(ID id);

    /**
     * 根据 ID 删除领域实体。
     *
     * @param id 唯一标识
     */
    void deleteById(ID id);
}
