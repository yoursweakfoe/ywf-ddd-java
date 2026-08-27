package com.yoursweakfoe.common.ddd.infrastructure.event.outbox;

import com.yoursweakfoe.common.ddd.domain.event.domain.DomainEvent;
import java.util.List;

/**
 * 领域事件 Outbox 捕获契约（SPI）—— 全链路 Outbox 可靠性规范的领域侧捕获面。
 *
 * <p><strong>全链路 Outbox 规范</strong>：所有领域事件强制经 Outbox 可靠投递——事件在
 * <strong>当前业务事务内</strong>写入 outbox 表（与业务写入同事务——可靠性锚点，
 * 「聚合状态已提交 ⇒ 事件必然已落库」；业务回滚则事件随行回滚），随后由框架排空器
 * （{@code OutboxRelay} 领域实例）在自有事务内派发给域内反应监听器。
 * <strong>事件与 Outbox 强绑定</strong>：聚合注册了事件但容器中无 {@link DomainEventOutboxStore}
 * Bean 时，{@code DomainEventFlusher} 直接抛错回滚业务写入——要么不用事件，要么带上 Outbox，
 * 不存在静默丢弃。
 *
 * <p><strong>框架提供缺省实现</strong>：{@link JdbcDomainEventOutboxStore}（标准表
 * {@code ddd_domain_event_outbox}，见 {@code resources/sql/ddd_domain_event_outbox.sql}），
 * 由 {@code OutboxAutoConfiguration} 在存在 {@code DataSource} 时自动装配
 * （{@code @ConditionalOnMissingBean}，业务可整体替换）。
 *
 * <p><strong>同事务义务</strong>：实现必须保证 {@link #appendAll} 与业务写入共享同一事务边界
 * （由调用方在事务内调用、实现不自行开启新事务，经 {@code DataSourceUtils} 复用事务绑定连接）；
 * 业务回滚则事件随行回滚。
 *
 * <p><strong>身份契约（捕获与投递之间唯一的跨边界约定）</strong>：
 * {@link DomainEvent#getEventId()} 是事件的幂等键，也是 outbox 行主键。重投递必须保持
 * eventId 稳定——消费端按它去重（at-least-once 是 Outbox 模式的固有语义）。
 * 载荷序列化复用 {@link DomainEventCodec}（信封四元组：eventId / eventType / payload /
 * occurredOn，身份跨重投稳定）。
 *
 * <p>线程安全要求：实现必须支持多线程并发调用（多个请求线程可能同时入箱）。
 *
 * @see JdbcDomainEventOutboxStore
 * @see DomainEventCodec
 */
public interface DomainEventOutboxStore {

    /**
     * 批量捕获领域事件（必须在当前业务事务内完成写入，不自行管理事务）。
     *
     * <p>事件以何种形态落库由实现决定；缺省实现按信封四元组 + 标准结构列写入
     * {@code ddd_domain_event_outbox}。
     *
     * @param events 待捕获事件（非空列表）
     */
    void appendAll(List<DomainEvent> events);
}
