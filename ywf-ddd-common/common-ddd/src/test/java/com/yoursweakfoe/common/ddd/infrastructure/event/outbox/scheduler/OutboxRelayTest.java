package com.yoursweakfoe.common.ddd.infrastructure.event.outbox.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.yoursweakfoe.common.ddd.domain.event.domain.DomainEvent;
import com.yoursweakfoe.common.ddd.fixtures.event.OrderCancelledEvent;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.DomainEventCodec;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.OutboxRow;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * OutboxRelay 测试 —— 纯策略排空引擎（行访问 SPI 注入，直调 {@code drain(n)}，不 sleep 等轮询）：
 * ① 成功路径：认领 → 派发（监听器收到，身份经 codec 重建）→ 同事务标记完成；重排空幂等；
 * ② 失败路径：派发异常 → 排空事务整体回滚（DB 副作用与标记完成一并撤销）→ 独立事务记失败簿记
 *    （attempts/next_retry_at/last_error 原样可断言）；
 * ③ 退避与死信：退避门内不可认领，达到 maxAttempts 转死信（WARN 告警）且永不再认领；
 * ④ 留存行不参与认领：已投递（软删留痕）与死信行均永不再认领——框架只写不清，留存不影响排空。
 *
 * <p>行访问用事务感知的内存桩（变更经事务同步器延迟到 beforeCommit、回滚即丢弃——与真实实现的
 * 「同事务义务」同构），事务基础设施用真实 {@link PlatformTransactionManager}，
 * REQUIRES_NEW 的提交 / 回滚语义由此可断言（桩状态 + 排空事务内的真实 DB 写入）。
 * 时钟用 {@link MutableClock}，退避 / 死信判定完全确定。
 */
@SpringBootTest(classes = OutboxRelayTest.TestConfig.class)
@ActiveProfiles("test")
@DisplayName("OutboxRelay —— 纯策略排空引擎：派发/每行事务性/退避重试/死信/留存行不参与认领")
class OutboxRelayTest {

    @Configuration
    @SpringBootApplication
    static class TestConfig {
        @Bean
        TestEventCapture testEventCapture() {
            return new TestEventCapture();
        }
    }

    /** 捕获经排空器派发的全部领域事件 */
    static class TestEventCapture {
        final List<DomainEvent> captured = new ArrayList<>();

        @EventListener
        public void onEvent(DomainEvent event) {
            captured.add(event);
        }
    }

    /**
     * 事务感知的内存行访问：簿记变更经事务同步器延迟到 beforeCommit 提交、回滚即丢弃，
     * 使「标记完成与派发同事务」「失败簿记独立事务」的 REQUIRES_NEW 语义可被断言。
     */
    static final class TxAwareRowAccess implements OutboxRowAccess {

        final List<StoredRow> rows = new ArrayList<>();

        /** 内存行（信封 + 簿记，可变字段供簿记更新）。 */
        static final class StoredRow {
            final String id;
            final String eventType;
            final String payload;
            final Instant occurredOn;
            int attempts;
            Instant nextRetryAt;
            String lastError;
            boolean dead;
            boolean done;
            Instant completedAt;

            StoredRow(String id, String eventType, String payload, Instant occurredOn) {
                this.id = id;
                this.eventType = eventType;
                this.payload = payload;
                this.occurredOn = occurredOn;
            }
        }

        StoredRow append(String id, String eventType, String payload, Instant occurredOn) {
            StoredRow row = new StoredRow(id, eventType, payload, occurredOn);
            rows.add(row);
            return row;
        }

        @Override
        public OutboxKind kind() {
            return OutboxKind.DOMAIN;
        }

        @Override
        public OutboxRow claimOne(OffsetDateTime dueBefore) {
            for (StoredRow row : rows) {
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
            inTransaction(() -> {
                StoredRow row = byId(id);
                row.done = true;
                row.completedAt = completedAt.toInstant();
            });
        }

        @Override
        public void recordFailure(String id, int newAttempts, OffsetDateTime nextRetryAt,
                                  String lastError, boolean dead, OffsetDateTime now) {
            inTransaction(() -> {
                StoredRow row = byId(id);
                row.attempts = newAttempts;
                row.nextRetryAt = nextRetryAt.toInstant();
                row.lastError = lastError;
                row.dead = dead;
            });
        }

        /** 有活动事务则延迟到 beforeCommit（回滚即丢弃），无事务则即时生效。 */
        private void inTransaction(Runnable mutation) {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void beforeCommit(boolean readOnly) {
                        mutation.run();
                    }
                });
            } else {
                mutation.run();
            }
        }

        private StoredRow byId(String id) {
            return rows.stream()
                    .filter(row -> row.id.equals(id))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Outbox row not found: " + id));
        }
    }

    /** 可推进时钟：退避 / 死信判定不依赖真实时间流逝 */
    static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    private final ApplicationEventPublisher applicationEventPublisher;
    private final PlatformTransactionManager transactionManager;
    private final DomainEventCodec codec;
    private final TestEventCapture eventCapture;
    private final JdbcTemplate jdbcTemplate;
    private final MutableClock clock = new MutableClock();
    private final TxAwareRowAccess rowAccess = new TxAwareRowAccess();
    private OutboxRelay relay;

    @Autowired
    OutboxRelayTest(ApplicationEventPublisher applicationEventPublisher,
                    PlatformTransactionManager transactionManager,
                    DomainEventCodec codec,
                    TestEventCapture eventCapture,
                    JdbcTemplate jdbcTemplate) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.transactionManager = transactionManager;
        this.codec = codec;
        this.eventCapture = eventCapture;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM orders.orders");
        eventCapture.captured.clear();
        rowAccess.rows.clear();
        relay = new OutboxRelay(rowAccess, transactionManager, publishingDispatcher(),
                3, Duration.ofMinutes(5), clock);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM orders.orders");
    }

    // ==================== 成功路径 + 幂等 ====================

    @Test
    @DisplayName("成功路径：drain 派发事件（身份经行重建），提交时标记完成；重排空幂等为 0")
    void drain_happyPath_dispatchesAndMarksDone_thenIdempotent() {
        OrderCancelledEvent event = appendRow(new OrderCancelledEvent(UUID.randomUUID(), "changed mind"));

        int processed = relay.drain(10);

        assertThat(processed).isEqualTo(1);
        // 事件在排空时才派发；身份经 codec 以 outbox 行重建（eventId 跨重投稳定）
        assertThat(eventCapture.captured).hasSize(1);
        DomainEvent dispatched = eventCapture.captured.get(0);
        assertThat(dispatched).isInstanceOf(OrderCancelledEvent.class);
        assertThat(dispatched.getEventId()).isEqualTo(event.getEventId());
        assertThat(((OrderCancelledEvent) dispatched).getOrderId()).isEqualTo(event.getOrderId());
        assertThat(dispatched.getOccurredOn()).isEqualTo(event.getOccurredOn());
        // 行在排空事务提交时标记完成
        assertThat(rowAccess.rows.get(0).done).isTrue();

        // 幂等重投：已完成行不再被认领
        assertThat(relay.drain(10)).isZero();
        assertThat(eventCapture.captured).hasSize(1);
    }

    // ==================== 每行事务性 ====================

    @Test
    @DisplayName("失败路径：派发事务回滚（DB 副作用与标记完成一并撤销），独立事务记失败簿记")
    void drain_dispatchFailure_transactionRolledBack_failureBookedInSeparateTx() {
        OrderCancelledEvent event = appendRow(new OrderCancelledEvent(UUID.randomUUID(), "boom case"));
        OutboxRelay failing = new OutboxRelay(rowAccess, transactionManager, row -> {
            // 排空事务内的真实 DB 副作用：随后抛错 → 必须随事务回滚
            jdbcTemplate.update("INSERT INTO orders.orders (id, status) VALUES (?, 'PENDING')", row.id());
            throw new IllegalStateException("boom: MQ unavailable");
        }, 2, Duration.ofMinutes(5), clock);

        assertThat(failing.drain(10)).isEqualTo(1);

        // 排空事务内的 DB 副作用已回滚（REQUIRES_NEW 包裹 claim+dispatch+markDone 的证据）
        Integer orderCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders.orders", Integer.class);
        assertThat(orderCount).isZero();
        // 行未标记完成（标记完成与派发同事务，回滚一并撤销）
        TxAwareRowAccess.StoredRow row = rowAccess.rows.get(0);
        assertThat(row.done).isFalse();
        // 失败簿记（独立事务已提交）：原样持久化框架计算值
        assertThat(row.attempts).isEqualTo(1);
        assertThat(row.nextRetryAt).isEqualTo(clock.instant().plusSeconds(2)); // 指数退避 2^1
        assertThat(row.lastError).contains("boom: MQ unavailable");
        assertThat(row.dead).isFalse(); // maxAttempts=2，尚未达上限
        assertThat(eventCapture.captured).isEmpty();
        assertThat(event.getEventId().toString()).isEqualTo(row.id);
    }

    // ==================== 退避 → 死信 ====================

    @Test
    @DisplayName("退避与死信：退避门内不可认领，达 maxAttempts 转死信（WARN）且永不再认领")
    void drain_backoffWindow_thenDeadLetterAtMaxAttempts() {
        appendRow(new OrderCancelledEvent(UUID.randomUUID(), "retry me"));
        OutboxRelay failing = new OutboxRelay(rowAccess, transactionManager,
                row -> {
                    throw new IllegalStateException("boom: MQ unavailable");
                }, 2, Duration.ofMinutes(5), clock);

        // 第一次失败：attempts=1，next_retry_at=+2s
        assertThat(failing.drain(10)).isEqualTo(1);

        // 退避窗口内（时钟未推进）：行不可认领
        assertThat(failing.drain(10)).isZero();

        // 推进时钟越过退避点 → 第二次失败 → attempts 达上限 → 转死信（WARN 告警）
        Logger relayLogger = (Logger) LoggerFactory.getLogger(OutboxRelay.class);
        ListAppender<ILoggingEvent> warnings = new ListAppender<>();
        warnings.start();
        relayLogger.addAppender(warnings);
        clock.advance(Duration.ofSeconds(3));
        int processed;
        try {
            processed = failing.drain(10);
        } finally {
            relayLogger.detachAppender(warnings);
        }
        assertThat(processed).isEqualTo(1);
        TxAwareRowAccess.StoredRow row = rowAccess.rows.get(0);
        assertThat(row.attempts).isEqualTo(2);
        assertThat(row.dead).isTrue();
        assertThat(row.done).isFalse(); // 死信不标记完成
        assertThat(warnings.list).anySatisfy(warning -> {
            assertThat(warning.getLevel()).isEqualTo(Level.WARN);
            assertThat(warning.getFormattedMessage()).contains("dead-lettered");
            assertThat(warning.getFormattedMessage()).contains("boom: MQ unavailable");
        });

        // 死信行永不再认领
        clock.advance(Duration.ofDays(1));
        assertThat(failing.drain(10)).isZero();
        assertThat(eventCapture.captured).isEmpty();
    }

    // ==================== 退避封顶 ====================

    @Test
    @DisplayName("指数退避封顶：min(2^attempts 秒, maxBackoff)")
    void recordFailure_backoffCappedAtMaxBackoff() {
        appendRow(new OrderCancelledEvent(UUID.randomUUID(), "cap me"));
        OutboxRelay failing = new OutboxRelay(rowAccess, transactionManager,
                row -> {
                    throw new IllegalStateException("boom");
                }, 3, Duration.ofSeconds(3), clock);

        // attempts=1 → 2^1=2s < 封顶 3s
        assertThat(failing.drain(10)).isEqualTo(1);
        assertThat(rowAccess.rows.get(0).nextRetryAt).isEqualTo(clock.instant().plusSeconds(2));

        // attempts=2 → 2^2=4s → 封顶 3s
        clock.advance(Duration.ofSeconds(2));
        assertThat(failing.drain(10)).isEqualTo(1);
        assertThat(rowAccess.rows.get(0).nextRetryAt).isEqualTo(clock.instant().plusSeconds(3));

        // attempts=3 = maxAttempts → 死信
        clock.advance(Duration.ofSeconds(3));
        assertThat(failing.drain(10)).isEqualTo(1);
        assertThat(rowAccess.rows.get(0).dead).isTrue();
    }

    // ==================== drain 循环 ====================

    @Test
    @DisplayName("drain 循环：单轮至多 batchSize 行，分轮排空余量")
    void drain_processesUpToBatchSizeRows() {
        appendRow(new OrderCancelledEvent(UUID.randomUUID(), "first"));
        appendRow(new OrderCancelledEvent(UUID.randomUUID(), "second"));
        appendRow(new OrderCancelledEvent(UUID.randomUUID(), "third"));

        assertThat(relay.drain(2)).isEqualTo(2);
        assertThat(eventCapture.captured).hasSize(2);
        assertThat(rowAccess.rows).extracting(row -> row.done).containsExactly(true, true, false);

        assertThat(relay.drain(2)).isEqualTo(1);
        assertThat(eventCapture.captured).hasSize(3);
        assertThat(relay.drain(2)).isZero();
    }

    // ==================== helpers ====================

    /** 与 OutboxAutoConfiguration 领域派发器同形：codec 重建事件 → 进程内发布 */
    private OutboxRelay.RowDispatcher publishingDispatcher() {
        return row -> applicationEventPublisher.publishEvent(
                codec.read(row.eventType(), row.payload(),
                        UUID.fromString(row.id()), row.occurredOn()));
    }

    /** 以捕获形态（信封四元组）向内存行访问建一行 */
    private OrderCancelledEvent appendRow(OrderCancelledEvent event) {
        rowAccess.append(event.getEventId().toString(), event.getClass().getName(),
                codec.write(event), event.getOccurredOn());
        return event;
    }
}
