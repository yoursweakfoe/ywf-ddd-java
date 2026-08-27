package com.yoursweakfoe.common.ddd.infrastructure.event.domain;

import com.yoursweakfoe.common.ddd.domain.event.domain.DomainEvent;
import com.yoursweakfoe.common.ddd.domain.event.publisher.DomainEventPublisher;
import com.yoursweakfoe.common.ddd.domain.model.AggregateRoot;
import com.yoursweakfoe.common.ddd.domain.model.Identifiable;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.OutboxStore;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 领域事件冲刷器 —— 在聚合持久化成功后，把聚合根暂存的领域事件「先清后交付」地冲刷出去。
 *
 * <p>从 {@code MybatisPlusPersistence} 抽出，承担事件冲刷的单一职责：聚合根持久化成功后，
 * 由本类快照其已注册的领域事件、清空暂存、再交付下游（先清后发，保证即使下游抛异常也不会重复交付）。
 *
 * <p><strong>框架领地边界</strong>（audit F-04 收口、领地收缩后定稿）：
 * <ul>
 *   <li><strong>Outbox 路径 = 只捕获、不投递</strong>（业务提供了 {@link OutboxStore} Bean 时激活）：
 *       事件在<strong>当前业务事务内</strong>写入 outbox 表（与业务写入同事务——可靠性锚点，
 *       「状态已提交 ⇒ 事件必然已落库」；业务回滚则事件随行回滚）。入箱之后的扫描 / 派发 /
 *       重试 / 死信归业务侧排空器或生态方案（MQ 事务消息 / CDC 等）——投递语义由投递拓扑
 *       决定，框架不做通用化假设。</li>
 *   <li><strong>直发路径</strong>（业务未提供 OutboxStore 时的降级）：注册 {@code afterCommit} 回调
 *       经 {@link DomainEventPublisher} 派发——监听器只看到已提交数据（修复「提交前监听器
 *       读到未提交数据」的时序缺陷）；进程在提交后窗口崩溃则事件丢失
 *       （<strong>at-most-once</strong>，无持久化锚点可言）。</li>
 * </ul>
 *
 * <p><strong>监听器契约（经排空器投递时）</strong>：投递发生在业务事务提交之后、
 * <strong>无活动事务</strong>的上下文，因此——
 * <ul>
 *   <li>监听器用普通 {@code @EventListener} 即可，不应使用
 *       {@code @TransactionalEventListener(AFTER_COMMIT)}（无事务可挂靠，默认不执行）</li>
 *   <li>监听器内的数据库写入须自带事务（{@code @Transactional(propagation = REQUIRES_NEW)}）</li>
 *   <li>at-least-once 是 Outbox 模式的固有语义，消费端以 {@code eventId} 幂等去重</li>
 * </ul>
 *
 * <p><strong>边界</strong>：本包（{@code infrastructure.event.domain}）仅负责<strong>领域事件</strong>的
 * 进程内交付。集成事件的收发不在此包：出站由 application 层 {@code Publisher} 投递（依赖 common-mq），
 * 入站由 adapter 层 {@code Consumer} 接收。
 *
 * <p>事件管线（OutboxStore 与发布者）全缺失时，丢弃事件并记录警告，不抛异常。
 */
@Slf4j
public class DomainEventFlusher {

    private final DomainEventPublisher publisher;
    private final OutboxStore outboxStore;

    public DomainEventFlusher(ObjectProvider<DomainEventPublisher> publisherProvider,
                              ObjectProvider<OutboxStore> outboxStoreProvider) {
        this.publisher = publisherProvider.getIfAvailable();
        this.outboxStore = outboxStoreProvider != null
                ? outboxStoreProvider.getIfAvailable() : null;
    }

    /**
     * 冲刷聚合根已注册的领域事件（先清后交付）。
     *
     * <p>若 domain 不是聚合根（无事件暂存），静默无操作。
     */
    public void publishAndClear(Identifiable<?> domain) {
        if (publisher == null && outboxStore == null) {
            if (domain instanceof AggregateRoot<?> ar && !ar.getDomainEvents().isEmpty()) {
                log.warn(
                        "No event pipeline available (publisher & outbox absent), {} event(s) discarded for entity ID: {}",
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
                deliver(snapshot);
            }
        }
    }

    /**
     * 冲刷外部构造的领域事件列表（按 ID 删除的事件工厂路径使用）。
     *
     * <p>与 {@link #publishAndClear(Identifiable)} 一致的容错语义：
     * 管线缺失时丢弃事件并记录警告，不抛异常。
     */
    public void publishAll(List<DomainEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        if (publisher == null && outboxStore == null) {
            log.warn("No event pipeline available (publisher & outbox absent), {} event(s) discarded", events.size());
            return;
        }
        deliver(events);
    }

    // ==================== 内部实现 ====================

    /**
     * 统一交付：优先 Outbox 捕获路径（同事务入箱，到此为止），否则直发降级路径
     * （注册 afterCommit 派发）。
     */
    private void deliver(List<DomainEvent> snapshot) {
        if (outboxStore != null) {
            // 捕获即边界：与业务写入同事务落库；后续投递归业务排空器
            outboxStore.appendAll(snapshot);
            log.debug("Captured {} domain event(s) into outbox", snapshot.size());
            return;
        }

        // 直发降级：提交后才真正派发（无事务场景立即派发）
        if (publisher == null) {
            log.warn("Outbox disabled and DomainEventPublisher missing, {} event(s) discarded", snapshot.size());
            return;
        }
        boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
        boolean syncActive = TransactionSynchronizationManager.isSynchronizationActive();
        if (!txActive || !syncActive) {
            if (txActive) {
                // 罕见：事务活动但未开同步（无法注册提交回调）。直发路径无持久化锚点，
                // 只能立即派发并明示降级
                log.warn("Transaction active without synchronization; publishing events before commit "
                        + "(direct path has no deferred-delivery mechanism)");
            }
            publisher.publishAll(snapshot);
        } else {
            registerAfterCommit(() -> publisher.publishAll(snapshot));
        }
    }

    private static void registerAfterCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    action.run();
                } catch (RuntimeException e) {
                    // afterCommit 异常不允许外溢（会破坏调用方事务完成流程）
                    log.error("Post-commit event delivery failed", e);
                }
            }
        });
    }
}
