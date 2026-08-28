package com.yoursweakfoe.common.ddd.application.event.outbox;

import com.yoursweakfoe.common.contract.dto.event.integration.IntegrationEvent;
import com.yoursweakfoe.common.ddd.domain.event.domain.DomainEvent;
import java.util.List;

/**
 * 集成事件 Outbox 捕获端口 —— 全链路 Outbox 可靠性规范的集成侧捕获面（应用层端口）。
 *
 * <p><strong>为何落在应用层</strong>：集成事件（{@link IntegrationEvent}）是 contract 层契约，
 * 领域事件（{@link DomainEvent}）属 domain 层——domain 依 R3 不得依赖 contract（聚合无法生产集成事件），
 * infrastructure 依 R1 不得回调应用业务组件；故本端口定义在应用层、由基础设施层实现
 * （与读侧 {@code QueryRepository} 端口同构的先例）。应用层出站 Publisher 在
 * <strong>领域排空事务内</strong>调用本端口，把翻译出的集成事件与业务写入同一原子单元入箱，
 * 关闭「领域事件已派发 → 集成事件投 MQ」之间的 dual-write 窗口。
 *
 * <p><strong>全链路 Outbox 规范</strong>：集成事件（最终 MQ 载荷）与捕获它的上下文
 * <strong>同事务</strong>写入 outbox 表，随后由框架排空器（{@code OutboxRelay} 集成实例）经
 * {@code IntegrationEventSender} 投递 MQ。{@code messageId = outbox 行 id}，下游按它幂等去重。
 *
 * <p><strong>同事务义务</strong>：实现必须保证 {@link #appendAll} 与调用方事务共享同一事务边界
 * （由调用方在事务内调用、实现不自行开启新事务）；调用方回滚则集成事件随行回滚。
 *
 * <p><strong>框架不提供缺省实现</strong>：使用方按本契约自行实现（参考实现见 sample-application）；
 * 标准 outbox 表结构为参考约定而非框架强制。
 *
 * <p>线程安全要求：实现必须支持多线程并发调用。
 *
 * @see com.yoursweakfoe.common.contract.dto.event.integration.IntegrationEvent
 */
public interface IntegrationEventOutboxStore {

    /**
     * 批量捕获集成事件（必须在调用方当前事务内完成写入，不自行管理事务）。
     *
     * <p>支持一个领域事件 fan-out 为 1..N 个集成事件；同一事务内一次性写入，
     * 与「领域行标记完成」原子，重复血缘不可能产生。
     *
     * @param source 产生本批集成事件的源领域事件（提供 {@code source_event_id} 溯源血缘）；
     *               入站集成事件再发出（无领域来源）时传 {@code null}
     * @param events 待捕获集成事件（非空列表）
     */
    void appendAll(DomainEvent source, List<IntegrationEvent> events);
}
