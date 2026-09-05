package com.yoursweakfoe.common.ddd.infrastructure.mybatis.mapper;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 框架级通用 Mapper 接口 —— 定义每个聚合手写 XML 必须提供的 7 条语句契约。
 *
 * <p>每个聚合的业务 Mapper（如 {@code OrderMapper extends DddMapper<OrderPO>}）配一份
 * <strong>手写 XML</strong>（namespace = 业务 Mapper 全限定名），实现下列全部语句 ID。
 * 泛型继承的方法按子接口 namespace 解析——每聚合 XML 独立成篇，无跨 namespace 共享，
 * 每条真正执行的 SQL 都是仓库一等公民：可见、可 grep、可 review。
 *
 * <h3>语句契约（XML 必须逐条满足）</h3>
 * <ul>
 *   <li><b>insert</b> — 枚举全部业务列（含 create_at / update_at 审计列，由
 *       {@code AuditFieldFiller} 保证非空）；<strong>不枚举</strong>逻辑删除列——
 *       靠 DB 默认 {@code FALSE}。业务铸造 ID 的聚合显式插入 id；
 *       数据库自增 ID 的聚合省略 id 列并声明 {@code useGeneratedKeys}。</li>
 *   <li><b>updateById</b> — <strong>全列 UPDATE</strong>（PO 必须由 Converter 完整装配，
 *       null 字段会被真实写为 NULL，不存在动态跳列语义）+ 审计列刷新
 *       （update_at = #{updateAt}）。逻辑删除聚合必须带 {@code AND deleted = false} 过滤。</li>
 *   <li><b>乐观锁条件</b> — 有版本列的聚合 UPDATE 语句必须携带
 *       {@code SET version = version + 1 ... WHERE id = #{id} AND version = #{version} AND deleted = false}
 *       ——版本条件由 SQL 文本自身承担，<strong>无任何运行时拦截器织入</strong>；
 *       影响行数 0 即版本冲突，由 {@code MybatisPersistence} 分类为
 *       {@code OptimisticLockConflictException}。PO 无 version 字段的聚合省略版本条件即可。</li>
 *   <li><b>selectById / selectByIds</b> — 查询列 + {@code WHERE ... AND deleted = false}
 *       （逻辑删除过滤写在 SQL 文本内，显式可见）。</li>
 *   <li><b>deleteById / deleteByIds</b> — 逻辑删除聚合写
 *       {@code UPDATE {table} SET deleted = true, update_at = #{now}[, updated_by = #{updatedBy}]
 *       WHERE id IN (...) AND deleted = false}；物理删除聚合写 {@code DELETE FROM ... WHERE id IN (...)}。
 *       审计参数 {@code now} / {@code updatedBy} 由基类填充，使逻辑删除同时刷新「最后状态」审计列；
 *       操作人列按聚合可选引用，建议以 {@code <if test="updatedBy != null">} 守卫避免无身份场景覆写已有值。</li>
 *   <li><b>existsById</b> — 轻量存在性探测（如 {@code SELECT EXISTS(SELECT 1 FROM ...
 *       WHERE id = #{id} AND deleted = false)}），必须<strong>恒返回一行</strong>（boolean），
 *       不加载完整行；{@code throwUpdateFailed} 的冲突分类依赖它。</li>
 * </ul>
 *
 * @param <PO> 持久化对象类型
 */
public interface DddMapper<PO> {

    /** 插入（XML 枚举全部业务列，不枚举逻辑删除列，靠 DB 默认值） */
    int insert(PO po);

    /**
     * 按主键全列更新。
     *
     * <p>有版本列的聚合 XML 必须携带乐观锁条件：
     * {@code SET version = version + 1 ... WHERE id = #{id} AND version = #{version} AND deleted = false}。
     *
     * @return 影响行数（0 = 主键不存在 / 已被逻辑删除 / 版本冲突）
     */
    int updateById(PO po);

    /** 按主键查询单条（XML 含逻辑删除过滤） */
    PO selectById(@Param("id") Serializable id);

    /** 按主键集合批量查询（XML foreach IN + 逻辑删除过滤） */
    List<PO> selectByIds(@Param("ids") List<Serializable> ids);

    /**
     * 按主键删除（逻辑删除聚合 = UPDATE 置位 + 审计刷新；物理删除聚合 = DELETE，忽略审计参数）。
     *
     * @param id        持久化主键
     * @param now       本次删除时刻（基类经注入 Clock 生成，供逻辑删除 SET 子句刷新 update_at）
     * @param updatedBy 本次删除操作人（基类经 CurrentUserProvider 宽松解析，无身份时为 null）
     * @return 影响行数
     */
    int deleteById(@Param("id") Serializable id,
                   @Param("now") OffsetDateTime now,
                   @Param("updatedBy") Object updatedBy);

    /**
     * 按主键集合批量删除（XML foreach；审计参数语义同 {@link #deleteById}）。
     *
     * @return 影响行数（调用方按 BEST_EFFORT 语义解读部分命中）
     */
    int deleteByIds(@Param("ids") List<Serializable> ids,
                    @Param("now") OffsetDateTime now,
                    @Param("updatedBy") Object updatedBy);

    /** 轻量存在性探测（SQL 恒返回一行 boolean；未过滤逻辑删除的聚合语义不成立，必须带 deleted = false 条件） */
    boolean existsById(@Param("id") Serializable id);
}
