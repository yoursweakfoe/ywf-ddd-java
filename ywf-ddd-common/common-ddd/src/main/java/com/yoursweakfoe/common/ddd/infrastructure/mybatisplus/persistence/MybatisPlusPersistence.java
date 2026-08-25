package com.yoursweakfoe.common.ddd.infrastructure.mybatisplus.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yoursweakfoe.common.ddd.domain.event.domain.DomainEvent;
import com.yoursweakfoe.common.ddd.domain.event.publisher.DomainEventPublisher;
import com.yoursweakfoe.common.ddd.domain.model.AggregateRoot;
import com.yoursweakfoe.common.ddd.domain.model.Identifiable;
import com.yoursweakfoe.common.ddd.infrastructure.converter.BasicConverter;
import com.yoursweakfoe.common.ddd.infrastructure.event.domain.DomainEventFlusher;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.GenericTypeResolver;

/**
 * MyBatis 仓储支撑类 —— 封装 MyBatis-Plus 持久化 + 领域事件发布。
 *
 * <p>本类<strong>组合</strong>持有 {@link BaseMapper} 而非继承 {@code ServiceImpl}，
 * 避免将 {@code save(PO)} / {@code updateById(PO)} / {@code removeById(...)} 等
 * 直接操作 PO 的底层方法泄漏为公开 API。领域对象的持久化必须经本类提供的
 * {@code saveDomain} / {@code updateDomain} / {@code removeDomain*} 方法，
 * 这些方法统一保证「validate() → 持久化 → 发布领域事件」的契约，
 * 绕过它们直接操作 PO 会丢失不变量校验与事件发布。
 *
 * <p>乐观锁由 MyBatis-Plus {@code OptimisticLockerInnerInterceptor} 处理，
 * 仅对 PO 上声明了 {@code @Version} 字段的实体生效，领域层无需感知版本号。
 *
 * <h3>内置行为契约</h3>
 * <ul>
 *   <li>save/update 前自动调用 {@code AggregateRoot.validate()}
 *   <li>update/delete 失败时抛出 {@link IllegalStateException}（不静默失败）
 *   <li>领域事件在持久化成功后发布（先清后发，保证原子性）——发布逻辑委托 {@link DomainEventFlusher}
 *   <li><b>事件事务语义</b>：事件在当前事务内同步发布（提交前）——
 *       {@code @EventListener} 监听器在同一事务内执行，抛异常会回滚主事务（适合强一致副作用）；
 *       提交后才应执行的副作用（通知/补偿/出站消息）请用
 *       {@code @TransactionalEventListener(phase = AFTER_COMMIT)}，且监听器内的数据库写入
 *       必须标注 {@code @Transactional(propagation = REQUIRES_NEW)}（原事务已完成，否则写入不会提交）
 *   <li>删除同样覆盖事件：实体删除发布聚合已注册事件，按 ID 删除通过事件工厂重载发布；
 *       带事件的批量删除（{@code removeDomainByIds(ids, factory)} / {@code removeDomains(list)}）
 *       先预查真实存在的 ID，<b>仅为实际删除的实体发布事件</b>——请求中不存在的 ID 静默跳过不报错
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
 *   removeDomainById(id, eventFactory)      → void（删除成功后发布 ID 事件）
 *   removeDomainByIds(ids)                  → void
 *   removeDomainByIds(ids, eventFactory)    → void（删除成功后逐 ID 发布事件）
 *   removeDomain(domain)                    → void（删除成功后发布聚合已注册事件）
 *   removeDomains(list)                     → void（删除成功后发布聚合已注册事件）
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

    /** 领域事件冲刷器（先清后发契约，委托独立组件以保持本类单一职责） */
    private final DomainEventFlusher eventFlusher;

    /** PO 类型（构造期经泛型解析固化，供主键列名反射） */
    private final Class<PO> poClass;

    // region 依赖注入
    /**
     * @param baseMapper MyBatis-Plus Mapper 实例（由子类构造器注入具体 Mapper 类型）
     * @param domainEventPublisherProvider 领域事件发布者（可选，容器中无此 Bean 时为 null，事件将被丢弃并记录警告）
     */
    @SuppressWarnings("unchecked")
    protected MybatisPlusPersistence(Mapper baseMapper,
                                     ObjectProvider<DomainEventPublisher> domainEventPublisherProvider) {
        this.baseMapper = baseMapper;
        this.eventFlusher = new DomainEventFlusher(domainEventPublisherProvider);
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
     * <p>契约：持久化前自动调用 validate()，持久化后发布领域事件。
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
        eventFlusher.publishAndClear(domain);
    }

    /**
     * 批量保存领域实体。
     *
     * <p><b>语义</b>：batch = 单事务循环（逐条 insert），非多行 VALUES SQL——每条聚合须独立
     * {@code validate()} + {@code registerEvent()} / 事件发布，多行 INSERT 无法触发逐聚合行为。
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

        eventFlusher.publishAndClear(domain);
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
     * {@code validate()} + {@code registerEvent()} / 事件发布，多行 UPDATE 无法触发逐聚合行为。
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
     * <p>本重载不发布领域事件（无 Domain 对象，无事件可发）。
     * 若删除具有业务含义（需通知其他聚合/服务），请使用
     * {@link #removeDomainById(Serializable, Function)} 事件工厂重载。
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
     * 根据 ID 删除领域实体，并在删除成功后发布事件工厂构造的领域事件。
     *
     * <p>适用于"只查 ID 不加载 Domain"的性能优化删除路径：
     * 删除事件通常只需携带聚合 ID，由事件工厂按 ID 构造（如 {@code id -> new OrderDeletedEvent(id)}），
     * 无需为发事件而加载完整聚合。
     *
     * <p>契约与 save/update 一致：先持久化（删除）成功，后发布事件。
     *
     * @param id           实体 ID
     * @param eventFactory 事件工厂（入参为被删除的 ID，返回值不能为 null）
     * @throws IllegalStateException 删除影响行数为 0 时（ID 不存在）
     */
    public void removeDomainById(ID id, Function<? super ID, ? extends DomainEvent> eventFactory) {
        removeDomainById(id);
        eventFlusher.publishAll(List.of(Objects.requireNonNull(
                eventFactory.apply(id), "eventFactory must not return null")));
    }

    /**
     * 批量删除领域实体。
     *
     * <p>本重载不发布领域事件。若删除具有业务含义，请使用
     * {@link #removeDomainByIds(Collection, Function)} 事件工厂重载。
     *
     * <p><b>事务边界上收</b>：本方法不声明 {@code @Transactional}，批量原子性由调用方（Handler）保证。
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
    }

    /**
     * 批量删除领域实体，并在删除成功后按 ID 逐个发布事件工厂构造的领域事件。
     *
     * <p><b>存在性过滤</b>：删除前预查真实存在的 ID（轻量主键列 SELECT），<strong>仅为
     * 实际删除的实体发布事件</strong>——请求中不存在（或已被逻辑删除）的 ID 不发事件、不报错。
     * 全部 ID 均不存在时仍抛 {@link IllegalStateException}（与无参重载一致的严格语义）。
     *
     * <p><b>并发窗口说明</b>：「存在性」以删除前的预查为准；若某实体在预查之后、DELETE 之前
     * 被并发删除，事件仍会发布（消费方幂等可兜底，与 MQ at-least-once 重投语义一致）。
     *
     * <p>适用于"只查 ID 不加载 Domain"的批量删除路径，事件与被删除的 ID 一一对应。
     *
     * <p><b>事务边界上收</b>：本方法不声明 {@code @Transactional}，批量原子性由调用方（Handler）保证。
     *
     * @param ids          实体 ID 集合（空集合直接返回，不发 SQL 不发事件）
     * @param eventFactory 事件工厂（入参为被删除的 ID，返回值不能为 null）
     * @throws IllegalStateException 全部 ID 均不存在（0 行删除）时
     */
    public void removeDomainByIds(Collection<ID> ids,
                                  Function<? super ID, ? extends DomainEvent> eventFactory) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<ID> existingIds = findExistingIds(ids);
        if (existingIds.isEmpty()) {
            throw new IllegalStateException("Batch DELETE affected 0 rows for IDs: " + ids);
        }
        removeDomainByIds(existingIds);
        eventFlusher.publishAll(existingIds.stream()
                .<DomainEvent>map(id -> Objects.requireNonNull(
                        eventFactory.apply(id), "eventFactory must not return null"))
                .toList());
    }

    /**
     * 删除领域实体（传入实体对象，内部提取 ID）。
     *
     * <p>契约与 save/update 一致：删除成功后自动发布聚合根已注册的领域事件（先清后发）。
     * 典型用法：{@code order.markCancelled()}（内部 registerEvent）→ {@code removeDomain(order)}。
     *
     * @throws IllegalStateException 删除影响行数为 0 时
     */
    public void removeDomain(Domain domain) {
        removeDomainById(domain.getId());
        eventFlusher.publishAndClear(domain);
    }

    /**
     * 批量删除领域实体（传入实体对象列表）。
     *
     * <p><b>存在性过滤</b>：与 {@link #removeDomainByIds(Collection, Function)} 一致——
     * 删除前预查真实存在的 ID，批量删除后<strong>仅为实际删除的聚合发布其已注册事件</strong>（先清后发）；
     * 传入列表中不存在（或已被逻辑删除）的聚合不发事件、不报错。全部不存在时抛 {@link IllegalStateException}。
     *
     * <p><b>事务边界上收</b>：本方法不声明 {@code @Transactional}，批量原子性由调用方（Handler）保证。
     */
    public void removeDomains(List<Domain> domainList) {
        if (domainList == null || domainList.isEmpty()) {
            return;
        }
        List<ID> requestedIds = domainList.stream().map(Domain::getId).toList();
        Set<ID> existingIds = Set.copyOf(findExistingIds(requestedIds));
        if (existingIds.isEmpty()) {
            throw new IllegalStateException("Batch DELETE affected 0 rows for IDs: " + requestedIds);
        }
        removeDomainByIds(existingIds);
        for (Domain domain : domainList) {
            if (domain.getId() == null || !existingIds.contains(domain.getId())) {
                continue; // 无 ID（从未持久化）或预查不存在的聚合：未删除，不发事件
            }
            eventFlusher.publishAndClear(domain);
        }
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

    /**
     * 预查集合中真实存在的领域 ID（供批量删除的事件过滤使用）。
     *
     * <p>一次仅查询主键列（{@code SELECT key FROM t WHERE key IN (...)}），不加载完整行；
     * 经 MyBatis-Plus 逻辑删除机制自动过滤已删除行——「存在」与「可被 DELETE 影响」语义一致。
     *
     * <p>主键值匹配按字符串形式归一（规避 JDBC 对整型的 Integer/Long 装箱差异）；
     * {@code toPersistenceId} 在本方法内确定性映射，请求中的重复 ID 天然去重。
     *
     * @return 入参中真实存在的领域 ID（保持入参遍历顺序）
     */
    private List<ID> findExistingIds(Collection<ID> ids) {
        Map<String, ID> keyToId = new LinkedHashMap<>();
        for (ID id : ids) {
            if (id == null) {
                // 无 ID 的领域对象从未持久化，视为不存在（跳过而非进 IN 子句）
                continue;
            }
            keyToId.put(String.valueOf(toPersistenceId(id)), id);
        }
        if (keyToId.isEmpty()) {
            return List.of();
        }
        List<Object> existingKeys = baseMapper.selectObjs(
                new QueryWrapper<PO>().select(keyColumn()).in(keyColumn(), keyToId.keySet()));
        return existingKeys.stream()
                .filter(Objects::nonNull)
                .map(key -> keyToId.get(String.valueOf(key)))
                .filter(Objects::nonNull)
                .toList();
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
