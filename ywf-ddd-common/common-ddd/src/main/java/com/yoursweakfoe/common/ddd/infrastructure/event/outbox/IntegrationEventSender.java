package com.yoursweakfoe.common.ddd.infrastructure.event.outbox;

/**
 * 集成事件投递者 SPI —— 框架排空器与具体 MQ 之间的接缝。
 *
 * <p>框架集成排空器（{@code OutboxRelay} 集成实例）从 {@code ddd_integration_event_outbox}
 * 认领一行、构造 {@link OutboxEnvelope}、调用本接口投递，<strong>投递成功后才标记该行完成</strong>
 * （at-least-once）。本接口只回答「把信封送到外部」，不关心认领 / 重试 / 死信（归排空器）。
 *
 * <p><strong>框架不内置 MQ 实现</strong>：common-mq 模块尚未建设，样例以日志占位实现
 * （{@code LoggingIntegrationEventSender}）；接入 RocketMQ / Kafka 时由业务或 common-mq
 * 提供实现（经 {@code @ConditionalOnMissingBean} 顶替），把 {@code messageId} 置消息头供消费端去重。
 *
 * <p><strong>失败语义</strong>：投递失败抛异常即可——排空器捕获后记 {@code attempts}/{@code last_error}、
 * 按退避重投，超限转死信。实现不应吞掉失败（否则排空器误判成功、标记完成、事件丢失）。
 *
 * <p>线程安全要求：实现必须支持多线程并发调用。
 *
 * @see OutboxEnvelope
 */
public interface IntegrationEventSender {

    /**
     * 将一个集成事件信封投递到外部（MQ / RPC 等，具体拓扑由实现决定）。
     *
     * @param envelope 待投递信封（非空）
     * @throws RuntimeException 投递失败（排空器据此重试 / 转死信）
     */
    void send(OutboxEnvelope envelope);
}
