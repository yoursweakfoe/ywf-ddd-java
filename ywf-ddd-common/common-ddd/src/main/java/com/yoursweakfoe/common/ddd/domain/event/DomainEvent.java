package com.yoursweakfoe.common.ddd.domain.event;

/**
 * 领域事件标记接口 —— 标识一个对象为领域内事件（DDD 中的 DomainEvent）。
 *
 * <p>领域事件表达「领域中已经发生、有业务意义的事实」，仅在进程内流转，不跨服务序列化、不进契约层。
 * 本接口是<strong>纯类型锚点</strong>（空标记）：框架只发行角色词汇——发布/订阅身份由
 * {@code DomainEventPublisher} / {@code DomainEventSubscriber} 标记定型，进程内派发机制
 * （典型为 Spring 事件薄包装）与跨服务翻译策略均由接入方自持，框架不内置任何派发。
 *
 * <p>实现类应命名为 {@code XxxEvent}（聚合名 + 过去式动词 + Event，如 {@code PaymentCapturedEvent}），
 * 位于 {@code {agg}/domain/event/}，字段一律 {@code final} —— 领域事件不可变。
 *
 * @see com.yoursweakfoe.common.contract.dto.event.IntegrationEvent
 * @see com.yoursweakfoe.common.ddd.domain.model.AggregateRoot
 * @see com.yoursweakfoe.common.ddd.application.event.publisher.DomainEventPublisher
 * @see com.yoursweakfoe.common.ddd.application.event.subscriber.DomainEventSubscriber
 */
public interface DomainEvent {}
