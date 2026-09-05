package com.yoursweakfoe.common.ddd.infrastructure.mybatisplus.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yoursweakfoe.common.ddd.domain.model.AggregateRoot;
import com.yoursweakfoe.common.ddd.domain.model.Identifiable;
import com.yoursweakfoe.common.ddd.infrastructure.converter.BasicConverter;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.springframework.core.GenericTypeResolver;

/**
 * MyBatis 仓储支撑类 —— 封装 MyBatis-Plus 持久化 + 乐观锁 + validate 自动调用。
 *
 * <p>本类<strong>组合</strong>持有 {@link BaseMapper} 而非继承 {@code ServiceImpl}，
 * 避免将 {@code save(PO)} / {@code updateById(PO)} / {@code removeById(...)} 等
 * 直接操作 PO 的底层方法泄漏为公开 API。领域对象的持久化必须经本类提供的
 * {@code saveDomain} / {@code updateDomain} / {@code removeDomain*} 方法，
 * 这些方法统一保证「validate() → 持久化」的契约，
 * 绕过它们直接操作 PO 会丢失不变量校验。
 *
 * <p>乐观锁由 MyBatis-Plus {@code OptimisticLockerInnerInterceptor} 处理，
 * 仅对 PO 上声明了 {@code @Version} 字段的实体生效，领域层无需感知版本号。
 *
 * <h3>内置行为契约</h3>
 * <ul>
 *   <li>save/update 前自动调用 {@code AggregateRoot.validate()}
 *   <li>update/delete 失败时抛出 {@link IllegalStateException}（不静默失败）
 *   <li>每次 update 均执行全量 UPDATE（保证 update_time 等审计字段始终刷新）
 * </ul>
 *
 * <h3>事务边界（上收至应用层）</h3>
 * <p>本类<strong>不声明任何 {@code @Transactional}</strong>——事务边界由应用层
 * （CommandHandler 标注 {@code @Transactional}）控制。包括批量方法在内，框架不做事务管理：
 * 批量操作需要整体原子性时，由调用方（Handler）在入口方法标注 {@code @Transactional} 包裹。
 *
 * <h3>泛型 ID 类型安全</h3>
 * <p>本类声明第 4 个泛型参数 {@code ID}（领域标识类型，须为 {@link Serializable}），
 * 使 {@code findDomainById} / {@code existsDomainById} / {@code removeDomainById*} 等方法的
 * 入参在编译期即为领域 ID 类型，而非退化为 {@code Serializable} 再靠调用方手工强转。
 *
 * <p>当领域 ID 与 PO 主键类型不一致时（如领域用 {@code UUID}、PO 用 {@code String} 存其文本），
 * 子类覆写 {@link #toPersistenceId(Serializable)} 完成映射；类型一致时无需覆写（默认透传）。
 *
 * <h3>读侧方法约定</h3>
 * <p>读侧（CQRS 查询）<strong>不经过本类</strong>：读路径绕过 domain，由独立的读端口
 * （application 层 {@code XxxQueryRepository}）直接从 PO 投影读 DTO。本类仅提供
 * <strong>写侧加载聚合</strong>所需的方法（{@code findDomainById} / {@code findDomainsByIds} /
 * {@code findDomainOneByCondition}），用于「load → 行为 → save」链路。
 *
 * <h3>公开方法清单</h3>
 * <pre>
 * 加载（写侧）：
 *   findDomainById(id)                → Optional&lt;Domain&gt;
 *   findDomainsByIds(ids)             → List&lt;Domain&gt;
 *   findDomainOneByCondition(wrapper) → Optional&lt;Domain&gt;（多条时抛 IllegalStateException）
 *   existsDomainById(id)              → boolean
 *
 * 保存：
 *   saveDomain(domain)                → void
 *   saveDomainBatch(list)             → void
 *
 * 更新：
 *   updateDomain(domain)              → void
 *   updateDomainBatch(list)           → void
 *
 * 删除：
 *   removeDomainById(id)                    → void
 *   removeDomainByIds(ids)                  → void
 *   removeDomain(domain)                    → void（按实体内部 ID 删除）
 *   removeDomains(list)                     → void
 * </pre>
 *
 * @param <Mapper> MyBatis-Plus Mapper 接口类型
 * @param <PO>     持久化对象类型
 * @param <Domain> 领域实体类型（必须实现 Identifiable）
 * @param <ID>     领域标识类型（须为 {@link Serializable}）
 */
@Slf4j
public abstract class MybatisPlusPersistence<
        Mapper extends BaseMapper<PO>,
        PO,
        Domain extends Identifiable<ID>,
        ID extends Serializable> {

    /** MyBatis-Plus Mapper（组合持有，不继承 ServiceImpl，避免泄漏底层 PO 直操方法） */
    protected final Mapper baseMapper;

    /** PO 类型（构造期经泛型解析固化，供主键列名反射） */
    private final Class<PO> poClass;

    // region 依赖注入
    /**
     * @param baseMapper MyBatis-Plus Mapper 实例（由子类构造器注入具体 Mapper 类型）
     */
    @SuppressWarnings("unchecked")
    protected MybatisPlusPersistence(Mapper baseMapper) {
        this.baseMapper = baseMapper;
        this.poClass = (Class<PO>) GenericTypeResolver
                .resolveTypeArguments(getClass(), MybatisPlusPersistence.class)[1];
    }
    // endregion

    /** 获取转换器（子类必须实现） */
    protected abstract BasicConverter<Domain, PO> getConverter();

    /**
     * 领域 ID → PO 主键的映射钩子。
     *
     * <p>默认透传（领域 ID 与 PO 主键同类型）。当二者类型不一致时（如领域用
     * {@code UUID}、PO 用 {@code String}），子类覆写本方法完成转换：
     *
     * <pre>{@code
     * @Override
     * protected Serializable toPersistenceId(UUID id) {
     *     return id.toString();
     * }
     * }</pre>
     *
     * @param id 领域标识
     * @return 可直接传给 {@link BaseMapper} 的持久化主键
     */
    protected Serializable toPersistenceId(ID id) {
        return id;
    }

    // region 写侧加载方法

    /** 根据 ID 查询领域实体 */
    public Optional<Domain> findDomainById(ID id) {
        PO po = baseMapper.selectById(toPersistenceId(id));
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(getConverter().toDomain(po));
    }

    /**
     * 根据 ID 集合批量加载领域实体。
     *
     * <p>返回顺序不保证与传入 ID 顺序一致。
     * 传入空集合时直接返回空 List，不发 SQL。
     */
    public List<Domain> findDomainsByIds(Collection<ID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Serializable> poIds = ids.stream().map(this::toPersistenceId).toList();
        return getConverter().toDomainList(baseMapper.selectByIds(poIds));
    }

    /**
     * 条件查询单个领域实体。
     *
     * <p>适用于按业务唯一键查询（如 orderNo、email）。
     * 若条件匹配多条记录，抛出 {@link IllegalStateException}（数据异常，不应静默返回第一条）。
     *
     * @return 领域实体，不存在时返回 empty
     * @throws IllegalStateException 条件匹配到多条记录时
     */
    public Optional<Domain> findDomainOneByCondition(LambdaQueryWrapper<PO> wrapper) {
        PO po;
        try {
            po = baseMapper.selectOne(wrapper);
        } catch (TooManyResultsException e) {
            // 安全约束（audit F-03）：SQL 片段不得进入异常消息——它会经全局异常处理器
            // 以 409 detail 回显给外部客户端，泄漏表/列结构。条件细节只记服务端日志。
            log.warn("findDomainOneByCondition matched multiple rows; sqlSegment={}", wrapper.getSqlSegment(), e);
            throw new IllegalStateException("Expected at most one row but found multiple", e);
        }
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(getConverter().toDomain(po));
    }

    /**
     * 判断指定 ID 的实体是否存在。
     *
     * <p>使用 {@code exists} 查询（SELECT 1 ... LIMIT 1，不加载完整行）。
     * 主键列名经 {@link TableInfoHelper} 反射 PO 的 {@code @TableId} 注解解析，子类无需指定。
     */
    public boolean existsDomainById(ID id) {
        return existsById(toPersistenceId(id));
    }

    // endregion

    // region 保存方法

    /**
     * 保存领域实体（INSERT）。
     *
     * <p>契约：持久化前自动调用 validate()。
     *
     * <p><b>事务说明</b>：本方法不声明 {@code @Transactional}，
     * 事务边界由应用层（Handler）控制。
     */
    public void saveDomain(Domain domain) {
        validateIfAggregate(domain);
        PO po = getConverter().toPO(domain);
        int rows = baseMapper.insert(po);
        if (rows == 0) {
            throw new IllegalStateException("INSERT failed for entity ID: " + domain.getId());
        }
    }

    /**
     * 批量保存领域实体。
     *
     * <p><b>语义</b>：batch = 单事务循环（逐条 insert），非多行 VALUES SQL——每条聚合须独立
     * {@code validate()}，多行 INSERT 无法触发逐聚合行为。
     * 需要真·多行性能优化时由调用方自行实现。
     *
     * <p><b>事务边界上收</b>：本方法不声明 {@code @Transactional}，批量原子性由调用方
     * （Handler）在入口方法标注 {@code @Transactional} 保证。未包裹事务时，逐条 INSERT
     * 各自提交，中途失败不回滚已插入的记录。
     */
    public void saveDomainBatch(List<Domain> domainList) {
        for (Domain domain : domainList) {
            saveDomain(domain);
        }
    }

    // endregion

    // region 更新方法

    /**
     * 更新领域实体（UPDATE）。
     *
     * <p>契约：
     * <ul>
     *   <li>持久化前自动调用 validate()
     *   <li><b>乐观锁版本冲突</b>（实体仍存在、版本不匹配）→ 抛 {@link OptimisticLockConflictException}
     *       ——调用方（如重试包装器）应按此类型识别可重试冲突，勿依赖消息文本
     *   <li><b>实体已被删除 / ID 不存在</b> → 抛普通 {@link IllegalStateException}
     *       （消息含 {@code entity not found}）——重试无意义，语义上区别对待
     *   <li>每次调用均执行 UPDATE（保证 update_time 等审计字段始终刷新）
     * </ul>
     *
     * <p><b>事务说明</b>：本方法不声明 {@code @Transactional}，
     * 事务边界由应用层（Handler）控制。
     *
     * @throws OptimisticLockConflictException 乐观锁版本冲突（实体仍存在）
     * @throws IllegalStateException UPDATE 影响行数为 0 且实体已不存在/被逻辑删除
     */
    public void updateDomain(Domain domain) {
        validateIfAggregate(domain);
        PO po = getConverter().toPO(domain);

        int rows = baseMapper.updateById(po);
        if (rows == 0) {
            throwUpdateFailed(domain);
        }
    }

    /**
     * UPDATE 影响行数为 0 的语义分类（audit F-01）。
     *
     * <p>影响行数为 0 本身无法区分「版本冲突」与「实体消失」——两者在 SQL 层都表现为
     * WHERE 未命中。仅在失败路径补一次存在性探测完成分类：该路径罕见，探测开销可忽略。
     * 存在性探测与分类之间理论上存在并发窗口（探测后即刻被删），按 not-found 处理属安全降级。
     *
     * <p>消息兼容期约定：两类消息均保留历史核心字样 {@code affected 0 rows}，
     * 未迁移到类型判断的旧消费方行为不变；新代码一律按异常类型区分，不匹配文本。
     */
    private void throwUpdateFailed(Domain domain) {
        if (existsDomainById(domain.getId())) {
            // 实体仍在 → 版本被并发事务推进 → 可安全重试的乐观锁冲突
            throw new OptimisticLockConflictException(
                    "UPDATE affected 0 rows for entity ID: %s (optimistic lock version conflict)"
                            .formatted(domain.getId()));
        }
        throw new IllegalStateException(
                "UPDATE affected 0 rows for entity ID: %s (entity not found or concurrently deleted)"
                        .formatted(domain.getId()));
    }

    /**
     * 批量更新领域实体。
     *
     * <p><b>语义</b>：batch = 单事务循环（逐条 update），非多行 UPDATE SQL——每条聚合须独立
     * {@code validate()}，多行 UPDATE 无法触发逐聚合行为。
     *
     * <p><b>事务边界上收</b>：本方法不声明 {@code @Transactional}，批量原子性由调用方（Handler）保证。
     */
    public void updateDomainBatch(List<Domain> domainList) {
        for (Domain domain : domainList) {
            updateDomain(domain);
        }
    }

    // endregion

    // region 删除方法

    /**
     * 根据 ID 删除领域实体。
     *
     * @throws IllegalStateException 删除影响行数为 0 时（ID 不存在）
     */
    public void removeDomainById(ID id) {
        int rows = baseMapper.deleteById(toPersistenceId(id));
        if (rows == 0) {
            throw new IllegalStateException("DELETE affected 0 rows for entity ID: " + id);
        }
    }

    /**
     * 批量删除领域实体。
     *
     * <p><b>事务边界上收</b>：本方法不声明 {@code @Transactional}，批量原子性由调用方（Handler）保证。
     *
     * <p><b>删除语义</b>：BEST_EFFORT——部分 ID 不存在时<b>静默跳过</b>（不报错），
     * 仅当全部 ID 均不存在时才抛异常。若需 STRICT 语义（任一不存在即报错），
     * 请逐条调用 {@link #removeDomainById(Serializable)}。
     */
    public void removeDomainByIds(Collection<ID> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<Serializable> poIds = ids.stream().map(this::toPersistenceId).toList();
        int rows = baseMapper.deleteByIds(poIds);
        if (rows == 0) {
            throw new IllegalStateException("Batch DELETE affected 0 rows for IDs: " + ids);
        }
        if (rows < poIds.size()) {
            log.warn("Batch DELETE partially succeeded: requested={}, deleted={}, skipped={}",
                    poIds.size(), rows, poIds.size() - rows);
        }
    }

    /**
     * 删除领域实体（传入实体对象，内部提取 ID）。
     *
     * @throws IllegalStateException 删除影响行数为 0 时
     */
    public void removeDomain(Domain domain) {
        removeDomainById(domain.getId());
    }

    /**
     * 批量删除领域实体（传入实体对象列表，内部提取 ID）。
     *
     * <p>删除语义与 {@link #removeDomainByIds(Collection)} 一致（BEST_EFFORT）；
     * 无 ID（从未持久化）的实体自动跳过。
     *
     * <p><b>事务边界上收</b>：本方法不声明 {@code @Transactional}，批量原子性由调用方（Handler）保证。
     */
    public void removeDomains(List<Domain> domainList) {
        if (domainList == null || domainList.isEmpty()) {
            return;
        }
        removeDomainByIds(domainList.stream().map(Domain::getId).filter(Objects::nonNull).toList());
    }

    // endregion

    // region 内部辅助

    /** 如果 domain 是聚合根，持久化前自动调用 validate() */
    private void validateIfAggregate(Domain domain) {
        if (domain instanceof AggregateRoot<?> aggregateRoot) {
            aggregateRoot.validate();
        }
    }

    /**
     * 判断指定 ID 的记录是否存在（轻量 EXISTS 查询，不加载完整行）。
     *
     * <p>主键列名经 {@link TableInfoHelper} 反射 PO 的 {@code @TableId} 注解解析，
     * 不写死列名：PO 字段叫 {@code id} 则为 {@code id}，叫 {@code orderId}
     * （配合驼峰映射）则为 {@code order_id}，或按 {@code @TableId("order_no")} 显式指定。
     */
    protected boolean existsById(Serializable id) {
        return baseMapper.exists(new QueryWrapper<PO>().eq(keyColumn(), id));
    }

    /** 反射 PO 的 {@code @TableId} 注解，返回真实主键列名。 */
    private String keyColumn() {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(poClass);
        if (tableInfo == null || tableInfo.getKeyColumn() == null) {
            throw new IllegalStateException(
                    "PO class " + poClass.getName()
                            + " is not registered as a MyBatis-Plus entity"
                            + " (missing @TableName or mapper scan); cannot resolve primary key column");
        }
        return tableInfo.getKeyColumn();
    }

    // endregion
}
