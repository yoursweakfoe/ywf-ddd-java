package com.yoursweakfoe.common.ddd.domain.repository.domain;

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
 * {@code MybatisPlusPersistence} 获得具体能力，而非继承框架实现类。
 *
 * <h3>设计原则</h3>
 * <ul>
 *   <li>本接口签名遵循语义版本（SemVer），major 版本才允许破坏性变更
 *   <li>新增能力通过 default 方法或扩展接口提供，不修改已有方法签名
 *   <li>业务代码只依赖本接口，不依赖 MyBatis/数据库相关实现类
 * </ul>
 *
 * <h3>本接口是「聚合生命周期」契约，不是「数据查询」契约</h3>
 * <p>五个方法构成最小生命周期集（load → 行为 → save/update/delete + exists 守卫）。
 * 两类「读」方法刻意<strong>不</strong>在此声明，落点另有所属：
 * <ul>
 *   <li><b>展示型读</b>（分页 / 条件查询投影 / findAll / count for UI）——
 *       CQRS 下拆至 application 层读端口
 *       {@code QueryRepository}
 *       （PO → 读 DTO 直接投影，绕过 domain），<strong>本接口不声明</strong>。</li>
 *   <li><b>决策型读</b>（领域逻辑需要读数据来做判断）——按领域语义写在<strong>业务子接口</strong>上
 *       （如 {@code findByOrderNo} / {@code countByCustomerAndStatus}），返回聚合根或计数，
 *       由 {@code MybatisPlusPersistence} 的 {@code findDomainOneByCondition} 支撑，
 *       不进本最小契约（避免用查询形状污染生命周期抽象）。</li>
 * </ul>
 *
 * @param <Domain> 领域对象类型（必须实现 {@link Identifiable}）
 * @param <ID>     标识类型
 */
public interface Repository<Domain extends Identifiable<ID>, ID> {

    // =============================================================
    // 结构性参考（注释版方法，非契约）
    // 取消每行行首 "// " 即为真实的方法声明形态。两类「读」落点不同：
    //
    // 【一】展示型读（分页 / 条件投影 / findAll / count-for-UI）→ 已拆出，
    //      归 application 层 read 端口（QueryRepository 标记），本接口不声明：
    //
    //     PageResult<XxxViewDTO> findPage(XxxPageQuery query);   // 分页投影
    //     Optional<XxxViewDTO> findOne(XxxQuery query);         // 条件投影（Query 封装条件）
    //     List<XxxViewDTO> findAll();                           // 全量投影
    //     long count(XxxQuery query);                           // 计数
    //
    // 【二】决策型读（领域逻辑要读数据做判断）→ 写在业务子接口（extends Repository），
    //      按领域语义命名、返回聚合根或计数，由 MybatisPlusPersistence.findDomainOneByCondition 支撑：
    //
    //     Optional<Domain> findByXxx(String someKey);                 // 幂等：业务唯一键查重
    //     int countByXxx(ID customerId, String status);              // 风控：满足条件的计数
    //     List<Domain> findRecentByXxx(ID customerId, int limit);    // 决策：最近 N 笔
    //
    // 注意：以上是「形状参考」，不返回 DTO、不承载分页（那些归【一】）。真实方法
    // 只应在业务子接口里按需添加，本最小生命周期契约保持五个方法不变。
    // =============================================================

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
