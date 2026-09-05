package com.yoursweakfoe.common.ddd.domain.event;

/**
 * 领域事件标记接口 —— 标识一个对象为领域内事件（DDD 中的 DomainEvent）。
 *
 * <p>领域事件表达「领域中已经发生、有业务意义的事实」，仅在进程内产生与消费，
 * 不跨服务序列化、不进契约层；需要跨服务协作时，由应用层将其翻译为
 * 集成事件（{@code IntegrationEvent}）后再行出站。
 *
 * <p>实现类应命名为 {@code XxxEvent}（聚合名 + 过去式动词 + Event，如 {@code OrderPlacedEvent}），
 * 位于 {@code {agg}/domain/event/}，字段一律 {@code final} —— 领域事件不可变。
 *
 * @see com.yoursweakfoe.common.contract.dto.event.IntegrationEvent
 * @see com.yoursweakfoe.common.ddd.domain.model.AggregateRoot
 */
public interface DomainEvent {}
