package com.yoursweakfoe.common.ddd.infrastructure.event.outbox;

import com.yoursweakfoe.common.ddd.domain.event.domain.DomainEvent;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.scheduler.OutboxKind;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.scheduler.OutboxRowAccess;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 测试用内存 Outbox 存储桩 —— 同一内存行集上同时实现捕获契约（{@link DomainEventOutboxStore}）
 * 与行访问 SPI（{@link OutboxRowAccess}），构成「捕获 → 排空」的全链路内存闭环，
 * 供此前依赖框架缺省捕获实现的测试上下文整体替换（不依赖数据库表）。
 *
 * <p>捕获侧：{@link #appendAll} 以信封四元组（id = eventId / event_type = 类全限定名 /
 * payload = codec 序列化 / occurredOn）建行，事件同时记入 {@link #captured}（供入箱断言）。
 * 排空侧：{@code claimOne} 按 FIFO 返回到期行，簿记方法原样落内存字段。
 *
 * <p>非事务感知（内存字段即时生效）；需要验证排空事务提交 / 回滚语义的测试
 * （如 {@code OutboxRelayTest}）应使用自身的事务感知桩。{@link #clear} 供 {@code @BeforeEach} 复位。
 */
public class InMemoryDomainEventOutboxStore implements DomainEventOutboxStore, OutboxRowAccess {

    /** 已捕获（入箱）的全部领域事件（append 顺序）。 */
    public final List<DomainEvent> captured = new ArrayList<>();

    private final List<Row> rows = new ArrayList<>();
    private final DomainEventCodec codec = new DomainEventCodec();

    @Override
    public void appendAll(List<DomainEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        for (DomainEvent event : events) {
            captured.add(event);
            rows.add(new Row(event.getEventId().toString(), event.getClass().getName(),
                    codec.write(event), event.getOccurredOn()));
        }
    }

    @Override
    public OutboxKind kind() {
        return OutboxKind.DOMAIN;
    }

    @Override
    public OutboxRow claimOne(OffsetDateTime dueBefore) {
        for (Row row : rows) {
            if (row.done || row.dead) {
                continue;
            }
            if (row.nextRetryAt != null && row.nextRetryAt.isAfter(dueBefore.toInstant())) {
                continue;
            }
            return new OutboxRow(row.id, row.eventType, row.payload, row.occurredOn, row.attempts);
        }
        return null;
    }

    @Override
    public void markDone(String id, OffsetDateTime completedAt) {
        Row row = byId(id);
        row.done = true;
        row.completedAt = completedAt.toInstant();
    }

    @Override
    public void recordFailure(String id, int newAttempts, OffsetDateTime nextRetryAt,
                              String lastError, boolean dead, OffsetDateTime now) {
        Row row = byId(id);
        row.attempts = newAttempts;
        row.nextRetryAt = nextRetryAt.toInstant();
        row.lastError = lastError;
        row.dead = dead;
    }

    /** 清空全部状态（含已捕获事件），供 {@code @BeforeEach} 复位。 */
    public void clear() {
        captured.clear();
        rows.clear();
    }

    private Row byId(String id) {
        return rows.stream()
                .filter(row -> row.id.equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Outbox row not found: " + id));
    }

    /** 内存行（信封 + 簿记，可变字段供簿记更新）。 */
    private static final class Row {
        final String id;
        final String eventType;
        final String payload;
        final java.time.Instant occurredOn;
        int attempts;
        java.time.Instant nextRetryAt;
        String lastError;
        boolean dead;
        boolean done;
        java.time.Instant completedAt;

        Row(String id, String eventType, String payload, java.time.Instant occurredOn) {
            this.id = id;
            this.eventType = eventType;
            this.payload = payload;
            this.occurredOn = occurredOn;
        }
    }
}
