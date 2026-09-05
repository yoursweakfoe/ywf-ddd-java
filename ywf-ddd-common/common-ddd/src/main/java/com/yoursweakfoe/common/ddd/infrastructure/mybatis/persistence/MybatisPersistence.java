package com.yoursweakfoe.common.ddd.infrastructure.mybatis.persistence;

import com.yoursweakfoe.common.ddd.domain.model.AggregateRoot;
import com.yoursweakfoe.common.ddd.domain.model.Identifiable;
import com.yoursweakfoe.common.ddd.infrastructure.converter.BasicConverter;
import com.yoursweakfoe.common.ddd.infrastructure.mybatis.config.AuditProperties;
import com.yoursweakfoe.common.ddd.infrastructure.mybatis.handler.AuditFieldFiller;
import com.yoursweakfoe.common.ddd.infrastructure.mybatis.handler.CurrentUserProvider;
import com.yoursweakfoe.common.ddd.infrastructure.mybatis.mapper.DddMapper;
import com.yoursweakfoe.common.exception.type.OptimisticLockConflictException;
import com.yoursweakfoe.common.exception.type.SilentWriteLossException;
import java.io.Serializable;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

/**
 * MyBatis 仓储支撑类 —— 组合手写 XML 通用语句（{@link DddMapper}）+ 乐观锁 + validate 自动调用。
 *
 * <p>本类<strong>组合</strong>持有 {@link DddMapper} 而非继承任何 Service 基类，
 * 避免将直接操作 PO 的底层方法泄漏为公开 API。领域对象的持久化必须经本类提供的
 * {@code saveDomain} / {@code updateDomain} / {@code removeDomain*} 方法，
 * 这些方法统一保证「validate() → 持久化」的契约，
 * 绕过它们直接操作 PO 会丢失不变量校验。
 *
 * <p>乐观锁<strong>无运行时拦截器</strong>——版本条件由每聚合手写 XML 的 UPDATE 语句自身携带：
 * {@code SET version = version + 1 ... WHERE id = #{id} AND version = #{version} AND deleted = false}。
 * 影响行数 0 即视为「WHERE 未命中」，本类经存在性探测分类为版本冲突或实体消失（见
 * {@link #updateDomain(Identifiable)}）。PO 无 version 字段的聚合，其 XML 省略版本条件即可。
 *
 * <h3>内置行为契约</h3>
 * <ul>
 *   <li>save/update 前自动调用 {@code AggregateRoot.validate()}
 *   <li>save/update 前自动经 {@link AuditFieldFiller} 显式填充审计字段
 *       （createAt / updateAt / createdBy / updatedBy，触发链透明可见）
 *   <li>写失败按语义三分通道抛异常（绝不静默失败）：乐观锁版本冲突 →
 *       {@link OptimisticLockConflictException}（可重试）；更新目标已并发消失 →
 *       {@link IllegalStateException}（业务竞态，409 可辩护）；INSERT/DELETE 影响 0 行 →
 *       {@link SilentWriteLossException}（写丢失级不可能状态，500+ERROR 告警通道，勿重试）
 *   <li>每次 update 均执行全量 UPDATE（保证 update_at 等审计字段始终刷新）
 *   <li>逻辑删除的审计刷新：{@code deleteById} / {@code deleteByIds} 的 SQL 参数携带
 *       {@code now}（经注入 Clock）与 {@code updatedBy}（经 {@link CurrentUserProvider} 宽松解析），
 *       由 XML 的 SET 子句写回审计列
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
 * <strong>写侧加载聚合</strong>所需的方法（{@code findDomainById} / {@code findDomainsByIds}），
 * 用于「load → 行为 → save」链路。业务唯一键的单条查询由子类以<strong>具名 Mapper 方法</strong>
 * 实现（普通 MyBatis selectOne 语义：命中多条时 MyBatis 核心抛 {@code TooManyResultsException}，
 * 经 mybatis-spring 翻译层到达调用方，子类按需识别包装为 {@link IllegalStateException}）
 * ——SQL 文本按业务命名，基类不设通用条件查询。
 *
 * <h3>公开方法清单</h3>
 * <pre>
 * 加载（写侧）：
 *   findDomainById(id)                → Optional&lt;Domain&gt;
 *   findDomainsByIds(ids)             → List&lt;Domain&gt;
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
 * @param <Mapper> 业务 Mapper 接口类型（必须扩展 {@link DddMapper}）
 * @param <PO>     持久化对象类型
 * @param <Domain> 领域实体类型（必须实现 Identifiable）
 * @param <ID>     领域标识类型（须为 {@link Serializable}）
 */
@Slf4j
public abstract class MybatisPersistence<
        Mapper extends DddMapper<PO>,
        PO,
        Domain extends Identifiable<ID>,
        ID extends Serializable> {

    /** 业务 Mapper（组合持有，不泄漏底层 PO 直操方法） */
    protected final Mapper mapper;

    // region 依赖注入
    /** 审计字段填充器（由基类构造，触发链显式可见） */
    private final AuditFieldFiller auditFieldFiller;

    /** 框架统一时间源（审计时间 + 逻辑删除审计参数） */
    private final Clock clock;

    /** 当前操作人 SPI（宽松可选——无实现 Bean 时删除审计参数解析为 null） */
    private final ObjectProvider<CurrentUserProvider> currentUserProvider;

    /**
     * @param mapper              业务 Mapper 实例（由子类构造器注入具体 Mapper 类型）
     * @param clock               框架统一时间源（{@code ClockAutoConfiguration} 提供，业务可覆盖）
     * @param auditProperties     审计字段名配置（{@code ywf.ddd.audit.*}）
     * @param currentUserProvider 当前操作人 SPI（无实现 Bean 时静默跳过操作人填充）
     */
    protected MybatisPersistence(Mapper mapper,
                                 Clock clock,
                                 AuditProperties auditProperties,
                                 ObjectProvider<CurrentUserProvider> currentUserProvider) {
        this.mapper = mapper;
        this.clock = clock;
        this.currentUserProvider = currentUserProvider;
        this.auditFieldFiller = new AuditFieldFiller(auditProperties, currentUserProvider, clock);
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
     * @return 可直接传给 {@link DddMapper} 的持久化主键
     */
    protected Serializable toPersistenceId(ID id) {
        return id;
    }

    // region 写侧加载方法

    /** 根据 ID 查询领域实体（SQL 文本内含逻辑删除过滤） */
    public Optional<Domain> findDomainById(ID id) {
        PO po = mapper.selectById(toPersistenceId(id));
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
        return getConverter().toDomainList(mapper.selectByIds(poIds));
    }

    /**
     * 判断指定 ID 的实体是否存在。
     *
     * <p>直接委托 {@link DddMapper#existsById(Serializable)}（XML 手写轻量存在性探测，
     * 不加载完整行、无注解反射解析），逻辑删除过滤由 SQL 文本承担。
     */
    public boolean existsDomainById(ID id) {
        return mapper.existsById(toPersistenceId(id));
    }

    // endregion

    // region 保存方法

    /**
     * 保存领域实体（INSERT）。
     *
     * <p>契约：持久化前自动调用 validate()，并经 {@link AuditFieldFiller#fillInsert(Object)}
     * 填充 createAt / updateAt /（可选）createdBy / updatedBy。
     *
     * <p><b>事务说明</b>：本方法不声明 {@code @Transactional}，
     * 事务边界由应用层（Handler）控制。
     *
     * @throws SilentWriteLossException INSERT 影响行数为 0——合法 INSERT 必落一行，
     *         0 行即写丢失/schema 事故的不可能状态，500+ERROR 告警通道，勿重试
     */
    public void saveDomain(Domain domain) {
        validateIfAggregate(domain);
        PO po = getConverter().toPO(domain);
        auditFieldFiller.fillInsert(po);
        int rows = mapper.insert(po);
        if (rows == 0) {
            throw new SilentWriteLossException(
                    "INSERT affected 0 rows for entity ID: " + domain.getId());
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
     *
     * <p><b>消费契约（分片责任在调用方）</b>：循环逐条意味着耗时与连接占用线性于批量大小——
     * 调用方<strong>必须自行分片（建议 ≤500 条/批）</strong>。框架刻意不设行数护栏：
     * 批大小上限属业务容量策略（不同聚合行宽、事务预算差异大），写死数字是业务规则渗入
     * 技术骨架（.agents/rules/04「Common 模块约束」零业务逻辑戒律）——以文档即契约约束调用方。
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
     *   <li>持久化前自动调用 validate()，并经 {@link AuditFieldFiller#fillUpdate(Object)}
     *       无条件刷新 updateAt +（可选）updatedBy
     *   <li><b>乐观锁版本冲突</b>（实体仍存在、版本不匹配——XML 的 {@code AND version = #{version}}
     *       未命中）→ 抛 {@link OptimisticLockConflictException}
     *       ——调用方（如重试包装器）应按此类型识别可重试冲突，勿依赖消息文本
     *   <li><b>实体已被删除 / ID 不存在</b> → 抛普通 {@link IllegalStateException}
     *       （消息含 {@code entity not found}）——重试无意义，语义上区别对待。
     *       <strong>范围围栏</strong>：此路径刻意<strong>不</strong>升级为
     *       {@link SilentWriteLossException}——「加载后被并发删除」属业务竞态
     *       （并发用户各自合法，409 冲突可辩护）；SilentWriteLoss 保留给
     *       INSERT/DELETE 影响 0 行的写丢失级不可能状态
     *   <li>每次调用均执行全量 UPDATE（保证 update_at 等审计字段始终刷新）
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
        auditFieldFiller.fillUpdate(po);

        int rows = mapper.updateById(po);
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
     *
     * <p><b>消费契约</b>：与 {@link #saveDomainBatch(List)} 同一分片纪律（≤500 条/批，
     * 护栏责任在调用方，框架不设行数上限的理由见彼处）。
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
     * <p>逻辑删除聚合：SQL 为 {@code UPDATE ... SET deleted = true, update_at = #{now}}；
     * 物理删除聚合：SQL 为 {@code DELETE}。审计参数由本类统一生成并传入，SQL 是否消费由聚合决定。
     *
     * @throws SilentWriteLossException 删除影响行数为 0（按存在的 ID 删除却未命中——写丢失级
     *         不可能状态；ID 是否曾存在属调用链上游职责，非本方法可分说的业务竞态）
     */
    public void removeDomainById(ID id) {
        int rows = mapper.deleteById(toPersistenceId(id), deleteNow(), deleteUpdatedBy());
        if (rows == 0) {
            throw new SilentWriteLossException("DELETE affected 0 rows for entity ID: " + id);
        }
    }

    /**
     * 批量删除领域实体。
     *
     * <p><b>事务边界上收</b>：本方法不声明 {@code @Transactional}，批量原子性由调用方（Handler）保证。
     *
     * <p><b>删除语义</b>：BEST_EFFORT——部分 ID 不存在时<b>静默跳过</b>（不报错），
     * 仅当全部 ID 均不存在时才抛 {@link SilentWriteLossException}（整批 0 命中即写丢失级
     * 不可能状态，与单条删除同一告警通道）。若需 STRICT 语义（任一不存在即报错），
     * 请逐条调用 {@link #removeDomainById(Serializable)}。
     */
    public void removeDomainByIds(Collection<ID> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<Serializable> poIds = ids.stream().map(this::toPersistenceId).toList();
        int rows = mapper.deleteByIds(poIds, deleteNow(), deleteUpdatedBy());
        if (rows == 0) {
            throw new SilentWriteLossException("Batch DELETE affected 0 rows for IDs: " + ids);
        }
        if (rows < poIds.size()) {
            log.warn("Batch DELETE partially succeeded: requested={}, deleted={}, skipped={}",
                    poIds.size(), rows, poIds.size() - rows);
        }
    }

    /**
     * 删除领域实体（传入实体对象，内部提取 ID）。
     *
     * @throws SilentWriteLossException 删除影响行数为 0 时
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

    /** 逻辑删除审计参数：本次删除时刻（经注入 Clock，与审计填充同一时间源） */
    private OffsetDateTime deleteNow() {
        return OffsetDateTime.now(clock);
    }

    /**
     * 逻辑删除审计参数：本次删除操作人。
     *
     * <p>与 {@link AuditFieldFiller} 的操作人填充同源——容器中无 {@link CurrentUserProvider}
     * Bean 或 provider 返回 null 时解析为 {@code null}；
     * XML 若不消费该参数（PO 无操作人列）或以 {@code <if>} 守卫之，则行为与「不填充」一致。
     */
    private Object deleteUpdatedBy() {
        CurrentUserProvider provider = currentUserProvider.getIfAvailable();
        return provider == null ? null : provider.currentUser();
    }
    // endregion
}
