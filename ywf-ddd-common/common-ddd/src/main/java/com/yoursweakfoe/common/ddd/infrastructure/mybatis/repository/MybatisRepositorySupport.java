package com.yoursweakfoe.common.ddd.infrastructure.mybatis.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yoursweakfoe.common.ddd.domain.event.DomainEvent;
import com.yoursweakfoe.common.ddd.domain.event.DomainEventPublisher;
import com.yoursweakfoe.common.ddd.domain.model.AggregateRoot;
import com.yoursweakfoe.common.ddd.domain.model.Identifiable;
import com.yoursweakfoe.common.ddd.domain.model.PageResult;
import com.yoursweakfoe.common.ddd.infrastructure.converter.BasicConverter;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.annotation.Transactional;

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
 *   <li>领域事件在持久化成功后发布（先清后发，保证原子性）
 *   <li><b>事件事务语义</b>：事件在当前事务内同步发布（提交前）——
 *       {@code @EventListener} 监听器在同一事务内执行，抛异常会回滚主事务（适合强一致副作用）；
 *       提交后才应执行的副作用（通知/补偿/出站消息）请用
 *       {@code @TransactionalEventListener(phase = AFTER_COMMIT)}，且监听器内的数据库写入
 *       必须标注 {@code @Transactional(propagation = REQUIRES_NEW)}（原事务已完成，否则写入不会提交）
 *   <li>删除同样覆盖事件：实体删除发布聚合已注册事件，按 ID 删除通过事件工厂重载发布
 *   <li>每次 update 均执行全量 UPDATE（保证 update_time 等审计字段始终刷新）
 * </ul>
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
 * <p>{@code findDomainPage} / {@code findDomainsByCondition} / {@code findDomainOneByCondition} /
 * {@code countByCondition} 等读侧方法依赖 MyBatis-Plus 的 {@code LambdaQueryWrapper}，
 * 属于基础设施层类型，<strong>不可</strong>出现在 domain 层的 {@code Repository} 接口。
 * 业务读侧用例应在 domain 层 {@code Repository} 接口用<strong>领域语言</strong>声明方法
 * （如 {@code Optional<Product> findByName(String name)}），infra 实现类内部调用本类工具方法实现：
 *
 * <pre>{@code
 * // domain 层接口（无 Wrapper 泄漏）
 * public interface ProductRepository extends Repository<Product, Long> {
 *     Optional<Product> findByName(String name);
 * }
 *
 * // infra 层实现（组合本类能力）
 * public Optional<Product> findByName(String name) {
 *     return findDomainOneByCondition(
 *         new LambdaQueryWrapper<ProductPO>().eq(ProductPO::getName, name));
 * }
 * }</pre>
 *
 * <h3>事务约定</h3>
 * <ul>
 *   <li>{@code saveDomain()}/{@code updateDomain()} 不声明 {@code @Transactional}
 *       —— 子类通过 {@code save()}/{@code update()} 自调用本类方法，Spring AOP 代理无法拦截自调用，
 *       因此事务必须由子类入口方法自行标注</li>
 *   <li>批量方法（{@code saveDomainBatch}/{@code updateDomainBatch}/{@code removeDomains}）
 *       由本类声明 {@code @Transactional} —— 它们由外部调用方（Handler）经代理触达，注解生效</li>
 * </ul>
 *
 * <h3>公开方法清单</h3>
 * <pre>
 * 查询：
 *   findDomainById(id)                → Optional&lt;Domain&gt;
 *   findDomainsByIds(ids)             → List&lt;Domain&gt;
 *   findDomainsByCondition(wrapper)   → List&lt;Domain&gt;
 *   findDomainOneByCondition(wrapper) → Optional&lt;Domain&gt;（多条时抛 IllegalStateException）
 *   existsDomainById(id)              → boolean
 *   countByCondition(wrapper)         → long
 *   findDomainPage(wrapper, num, size)→ PageResult&lt;Domain&gt;
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
public abstract class MybatisRepositorySupport<
        Mapper extends BaseMapper<PO>,
        PO,
        Domain extends Identifiable<ID>,
        ID extends Serializable> {

    /** MyBatis-Plus Mapper（组合持有，不继承 ServiceImpl，避免泄漏底层 PO 直操方法） */
    protected final Mapper baseMapper;

    private final DomainEventPublisher domainEventPublisher;

    // region 依赖注入
    /**
     * @param baseMapper MyBatis-Plus Mapper 实例（由子类构造器注入具体 Mapper 类型）
     * @param domainEventPublisherProvider 领域事件发布者（可选，容器中无此 Bean 时为 null，事件将被丢弃并记录警告）
     */
    protected MybatisRepositorySupport(Mapper baseMapper,
                                       ObjectProvider<DomainEventPublisher> domainEventPublisherProvider) {
        this.baseMapper = baseMapper;
        this.domainEventPublisher = domainEventPublisherProvider.getIfAvailable();
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

    // region 查询方法

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

    /** 条件查询领域实体列表 */
    public List<Domain> findDomainsByCondition(LambdaQueryWrapper<PO> wrapper) {
        return getConverter().toDomainList(baseMapper.selectList(wrapper));
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
            throw new IllegalStateException(
                    "Expected at most one row but found multiple for condition: " + wrapper.getSqlSegment(), e);
        }
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(getConverter().toDomain(po));
    }

    /**
     * 判断指定 ID 的实体是否存在。
     *
     * <p>默认使用 {@code exists} 查询（SELECT 1 ... LIMIT 1，不加载完整行）。
     * 子类可覆写 {@link #existsById(Serializable)} 以自定义 ID 列名。
     */
    public boolean existsDomainById(ID id) {
        return existsById(toPersistenceId(id));
    }

    /** 条件统计数量 */
    public long countByCondition(LambdaQueryWrapper<PO> wrapper) {
        return baseMapper.selectCount(wrapper);
    }

    /**
     * 分页查询领域实体。
     *
     * <p>内部使用 MyBatis-Plus {@code Page} 执行分页，出口翻译为 {@link PageResult}，
     * 调用方无需依赖 MyBatis-Plus 分页类型。
     *
     * @param wrapper  查询条件
     * @param pageNum  页码（从 1 开始）
     * @param pageSize 每页大小
     * @return 分页结果（Domain 列表 + 分页元数据）
     */
    public PageResult<Domain> findDomainPage(LambdaQueryWrapper<PO> wrapper, int pageNum, int pageSize) {
        // 防御性下限：仅拦截非法值（≤0），不截断上限——上限由契约层 @Max 或业务实现类自行决定
        int safePageNum = Math.max(1, pageNum);
        int safePageSize = Math.max(1, pageSize);
        Page<PO> mpPage = baseMapper.selectPage(new Page<>(safePageNum, safePageSize), wrapper);
        List<Domain> domains = getConverter().toDomainList(mpPage.getRecords());
        return new PageResult<>(domains, mpPage.getTotal(), safePageNum, safePageSize);
    }

    // endregion

    // region 保存方法

    /**
     * 保存领域实体（INSERT）。
     *
     * <p>契约：持久化前自动调用 validate()，持久化后发布领域事件。
     *
     * <p><b>事务说明</b>：本方法不声明 {@code @Transactional}。
     * 子类 Repository 的 {@code save()} 覆写方法应自行标注
     * {@code @Transactional(rollbackFor = Exception.class)}，
     * 因为 Spring AOP 代理无法拦截自调用（self-invocation）。
     */
    public void saveDomain(Domain domain) {
        validateIfAggregate(domain);
        PO po = getConverter().toPO(domain);
        int rows = baseMapper.insert(po);
        if (rows == 0) {
            throw new IllegalStateException("INSERT failed for entity ID: " + domain.getId());
        }
        publishAndClearEvents(domain);
    }

    /**
     * 批量保存领域实体（方法内保证事务，若中途失败则整体回滚）。
     *
     * <p><b>事务自调用说明</b>：本方法内部循环调用 {@code saveDomain()}（自调用，不经代理），
     * 事务完全由本方法的 {@code @Transactional} 控制。外部调用方（Handler）经代理触达本方法，
     * 注解生效。请勿在子类中绕过本方法直接循环调用 {@code saveDomain()} 并期望批量事务。
     */
    @Transactional(rollbackFor = Exception.class)
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
     *   <li>乐观锁冲突时抛出 {@link IllegalStateException}
     *   <li>每次调用均执行 UPDATE（保证 update_time 等审计字段始终刷新）
     * </ul>
     *
     * <p><b>事务说明</b>：本方法不声明 {@code @Transactional}。
     * 子类 Repository 的 {@code update()} 覆写方法应自行标注
     * {@code @Transactional(rollbackFor = Exception.class)}，
     * 因为 Spring AOP 代理无法拦截自调用（self-invocation）。
     *
     * @throws IllegalStateException UPDATE 影响行数为 0 时（可能原因：乐观锁版本冲突，或实体已被删除/ID 不存在）
     */
    public void updateDomain(Domain domain) {
        validateIfAggregate(domain);
        PO po = getConverter().toPO(domain);

        int rows = baseMapper.updateById(po);
        if (rows == 0) {
            throw new IllegalStateException(
                    "UPDATE affected 0 rows for entity ID: " + domain.getId()
                            + " (possible cause: concurrent modification or entity not found)");
        }

        publishAndClearEvents(domain);
    }

    /**
     * 批量更新领域实体（方法内保证事务，若中途失败则整体回滚）。
     *
     * <p><b>事务自调用说明</b>：同 {@link #saveDomainBatch(List)}，
     * 内部循环调用 {@code updateDomain()}（自调用），事务由本方法统一控制。
     */
    @Transactional(rollbackFor = Exception.class)
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
        publishEvents(List.of(Objects.requireNonNull(
                eventFactory.apply(id), "eventFactory must not return null")));
    }

    /**
     * 批量删除领域实体（方法内保证事务，若中途失败则整体回滚）。
     *
     * <p>本重载不发布领域事件。若删除具有业务含义，请使用
     * {@link #removeDomainByIds(Collection, Function)} 事件工厂重载。
     */
    @Transactional(rollbackFor = Exception.class)
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
     * 批量删除领域实体，并在删除成功后按 ID 逐个发布事件工厂构造的领域事件（方法内保证事务）。
     *
     * <p>适用于"只查 ID 不加载 Domain"的批量删除路径，事件与 ID 一一对应。
     *
     * @param ids          实体 ID 集合（空集合直接返回，不发 SQL 不发事件）
     * @param eventFactory 事件工厂（入参为被删除的 ID，返回值不能为 null）
     * @throws IllegalStateException 批量删除影响行数为 0 时
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeDomainByIds(Collection<ID> ids,
                                  Function<? super ID, ? extends DomainEvent> eventFactory) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        removeDomainByIds(ids);
        publishEvents(ids.stream()
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
        publishAndClearEvents(domain);
    }

    /**
     * 批量删除领域实体（传入实体对象列表，方法内保证事务）。
     *
     * <p>批量删除成功后，逐个发布各聚合根已注册的领域事件（先清后发）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeDomains(List<Domain> domainList) {
        if (domainList == null || domainList.isEmpty()) {
            return;
        }
        List<ID> ids = domainList.stream().map(Domain::getId).toList();
        removeDomainByIds(ids);
        for (Domain domain : domainList) {
            publishAndClearEvents(domain);
        }
    }

    // endregion

    // region 领域事件

    /**
     * 发布聚合根的领域事件。
     *
     * <p>采用"先清后发"策略：先清空事件列表，再逐个发布。
     * 即使某个 EventListener 抛异常，事件也不会被重复发布。
     */
    private void publishAndClearEvents(Domain domain) {
        if (domainEventPublisher == null) {
            if (domain instanceof AggregateRoot<?> ar && !ar.getDomainEvents().isEmpty()) {
                log.warn(
                        "DomainEventPublisher not available, {} event(s) discarded for entity ID: {}",
                        ar.getDomainEvents().size(),
                        domain.getId());
            }
            return;
        }
        if (domain instanceof AggregateRoot<?> aggregateRoot) {
            List<DomainEvent> events = aggregateRoot.getDomainEvents();
            if (!events.isEmpty()) {
                List<DomainEvent> snapshot = List.copyOf(events);
                aggregateRoot.clearDomainEvents();
                domainEventPublisher.publishAll(snapshot);
            }
        }
    }

    /**
     * 发布外部构造的领域事件（按 ID 删除的事件工厂路径使用）。
     *
     * <p>与 {@link #publishAndClearEvents} 一致的容错语义：
     * publisher 缺失时丢弃事件并记录警告，不抛异常。
     */
    private void publishEvents(List<DomainEvent> events) {
        if (events.isEmpty()) {
            return;
        }
        if (domainEventPublisher == null) {
            log.warn("DomainEventPublisher not available, {} event(s) discarded", events.size());
            return;
        }
        domainEventPublisher.publishAll(events);
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
     * <p>默认使用主键列名 {@code id}。若 PO 的主键列名不是 {@code id}，
     * 子类应覆写本方法指定正确的列名。
     */
    protected boolean existsById(Serializable id) {
        return baseMapper.exists(new QueryWrapper<PO>().eq("id", id));
    }

    // endregion
}
