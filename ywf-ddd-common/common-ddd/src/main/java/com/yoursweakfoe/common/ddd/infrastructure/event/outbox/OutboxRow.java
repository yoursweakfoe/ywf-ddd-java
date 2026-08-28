package com.yoursweakfoe.common.ddd.infrastructure.event.outbox;

import java.time.Instant;

/**
 * 认领到的一行（信封 + 当前重试计数）—— 排空引擎与行访问 SPI 之间的行载体。
 *
 * <p>字段即「捕获与投递之间跨边界约定」在排空侧的最小集：{@code id} 在领域侧 = eventId、
 * 集成侧 = 未来 MQ messageId（跨重投稳定的幂等键锚点）；{@code occurredOn} 为原始发生时间
 * （领域侧身份重建输入）；{@code attempts} 为当前累计重试次数（失败簿记 +1 的基数）。
 * 本 record 不可变。
 *
 * @see OutboxRowAccess
 * @see com.yoursweakfoe.common.ddd.infrastructure.event.outbox.scheduler.OutboxRelay
 */
public record OutboxRow(String id, String eventType, String payload, Instant occurredOn, int attempts) {
}
