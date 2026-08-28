package com.yoursweakfoe.common.ddd.infrastructure.event.outbox.scheduler;

/**
 * Outbox 行类别 —— 行访问 SPI（{@link OutboxRowAccess}）的派发走向锚点。
 *
 * <p>自动装配按本类别为每个行访问选择排空派发回调：{@link #DOMAIN} 经
 * {@code DomainEventPublisher} 进程内派发（经 codec 以行身份重建事件）；
 * {@link #INTEGRATION} 经 {@code IntegrationEventSender} 构造信封投 MQ。
 * 同类别的多个实现各自独立装配排空引擎（支持分表）。
 */
public enum OutboxKind {
    DOMAIN,
    INTEGRATION
}
