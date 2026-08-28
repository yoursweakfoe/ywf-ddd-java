package com.yoursweakfoe.common.ddd.infrastructure.event.domain;

import com.yoursweakfoe.common.ddd.domain.event.domain.DomainEvent;
import com.yoursweakfoe.common.ddd.domain.model.AggregateRoot;
import com.yoursweakfoe.common.ddd.domain.model.Identifiable;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.DomainEventOutboxStore;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 领域事件 Outbox 捕获器 —— 在聚合持久化成功后，把聚合根暂存的领域事件「先清后入箱」地捕获。
 *
 * <p>从 {@code MybatisPlusPersistence} 抽出，承担事件捕获的单一职责：聚合根持久化成功后，
 * 由本类快照其已注册的领域事件、清空暂存、再捕获入 Outbox（先清后捕，保证即使下游抛异常也不会重复捕获）。
 *
 * <p><strong>全链路 Outbox 可靠性规范</strong>：领域事件强制经 Outbox 捕获
 * （{@link DomainEventOutboxStore}），在<strong>当前业务事务内</strong>写入 outbox 表
 * （与业务写入同事务——可靠性锚点，「状态已提交 ⇒ 事件必然已落库」；业务回滚则事件随行回滚），
 * 随后由框架排空器（{@code OutboxRelay} 领域实例）在自有事务内派发给域内反应监听器。
 *
 * <p><strong>fail-fast（事件强制要求 Outbox）</strong>：聚合注册了事件但容器中无
 * {@link DomainEventOutboxStore} Bean 时，抛 {@link IllegalStateException} 回滚业务写入——
 * 要么不用事件，要么带上 Outbox，<strong>不存在静默丢弃，也不存在直发降级</strong>。
 *
 * <p><strong>监听器契约（经排空器投递时）</strong>：派发发生在排空器自有事务内（有活动事务），因此——
 * <ul>
 *   <li>监听器用普通 {@code @EventListener}，数据库写入用普通 {@code @Transactional}（加入排空器事务）；
 *       <strong>禁用</strong> {@code REQUIRES_NEW} 与 {@code @Async}——二者都会撕碎
 *       「内部反应 + 集成入箱 + 标记完成」的原子性，重试时产生双份副作用</li>
 *   <li>监听器不做任何非事务副作用（HTTP / 直发 MQ）——对外通知一律经集成 Outbox 捕获</li>
 *   <li>at-least-once 是 Outbox 模式的固有语义，消费端以 {@code eventId} 幂等去重</li>
 * </ul>
 *
 * <p><strong>边界</strong>：本类仅负责领域事件的<strong>同事务捕获</strong>，派发归排空器；
 * 集成事件的收发不在此包：出站由 application 层 {@code Capture} 翻译 + 集成 Outbox 捕获，
 * 入站由 adapter 层 {@code Consumer} 接收。
 */
@Slf4j
public class DomainEventCapture {

    /** 领域事件 Outbox 捕获存储（可能为 null —— 无 Outbox 时 fail-fast） */
    private final DomainEventOutboxStore outboxStore;

    public DomainEventCapture(ObjectProvider<DomainEventOutboxStore> outboxStoreProvider) {
        this.outboxStore = outboxStoreProvider != null ? outboxStoreProvider.getIfAvailable() : null;
    }

    /**
     * 捕获聚合根已注册的领域事件（先清后入箱）。
     *
     * <p>若 domain 不是聚合根（无事件暂存）或无已注册事件，静默无操作。
     *
     * @throws IllegalStateException 有事件但无 {@link DomainEventOutboxStore}（fail-fast，回滚业务写入）
     */
    public void captureAndClear(Identifiable<?> domain) {
        if (domain instanceof AggregateRoot<?> aggregateRoot) {
            List<DomainEvent> events = aggregateRoot.getDomainEvents();
            if (!events.isEmpty()) {
                List<DomainEvent> snapshot = List.copyOf(events);
                aggregateRoot.clearDomainEvents();
                capture(snapshot);
            }
        }
    }

    /**
     * 捕获外部构造的领域事件列表（按 ID 删除的事件工厂路径使用）。
     *
     * @throws IllegalStateException 有事件但无 {@link DomainEventOutboxStore}（fail-fast，回滚业务写入）
     */
    public void captureAll(List<DomainEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        capture(events);
    }

    // ==================== 内部实现 ====================

    /**
     * Outbox 同事务捕获。无 Outbox 但有事件 → 抛错回滚业务写入（用事件必须配 Outbox）。
     */
    private void capture(List<DomainEvent> snapshot) {
        if (outboxStore == null) {
            throw new IllegalStateException(
                    "Domain event(s) registered but no DomainEventOutboxStore bean is available. "
                            + "Events mandate Outbox reliability: either register a DomainEventOutboxStore bean "
                            + "(reference implementation: sample-application) or do not register events. "
                            + "Discarded event count: " + snapshot.size());
        }
        outboxStore.appendAll(snapshot);
        log.debug("Captured {} domain event(s) into outbox", snapshot.size());
    }
}
