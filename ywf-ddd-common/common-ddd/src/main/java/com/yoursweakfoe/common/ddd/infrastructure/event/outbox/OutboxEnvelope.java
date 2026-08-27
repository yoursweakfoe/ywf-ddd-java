package com.yoursweakfoe.common.ddd.infrastructure.event.outbox;

import java.time.Instant;
import java.util.UUID;

/**
 * Outbox 集成事件投递信封 —— 排空器交给 {@link IntegrationEventSender} 的最小投递单元。
 *
 * <p>信封字段即「捕获与投递之间唯一的跨边界约定」在集成侧的形态：
 * <ul>
 *   <li>{@code messageId} — 集成 outbox 行 id，跨重投稳定（它就是行本身）；下游按它幂等去重</li>
 *   <li>{@code eventType} — 集成事件类全限定名（反序列化锚点）</li>
 *   <li>{@code payload} — 集成事件 JSON 载荷（最终 MQ 载荷）</li>
 *   <li>{@code occurredOn} — 捕获时间（UTC）</li>
 * </ul>
 *
 * <p>排空器按行构造本信封并交给 sender；sender 把信封翻译为具体 MQ 消息（messageId 置消息头，
 * 供消费端去重）。本 record 不可变。
 *
 * @see IntegrationEventSender
 */
public record OutboxEnvelope(UUID messageId, String eventType, String payload, Instant occurredOn) {
}
