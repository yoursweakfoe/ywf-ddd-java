package com.yoursweakfoe.common.ddd.infrastructure.event.outbox.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.yoursweakfoe.common.ddd.application.event.outbox.IntegrationEventOutboxStore;
import com.yoursweakfoe.common.ddd.domain.event.domain.DomainEvent;
import com.yoursweakfoe.common.ddd.fixtures.event.OrderCancelledEvent;
import com.yoursweakfoe.common.ddd.fixtures.event.OrderPlacedEvent;
import com.yoursweakfoe.common.ddd.fixtures.event.StockDeductedEvent;
import com.yoursweakfoe.common.ddd.fixtures.event.StockDeductedIntegrationEvent;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.DomainEventCodec;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.DomainEventOutboxStore;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import org.springframework.transaction.support.TransactionTemplate;

/**
 * OutboxRelay 测试 —— 确定性驱动排空引擎（直调 {@code drain(n)}，不 sleep 等轮询）：
 * ① 成功路径：认领 → 派发（监听器收到，身份经 codec 重建）→ 同事务标记完成；重排空幂等；
 * ② 失败路径：派发异常 → 事务回滚不标记完成 → attempts/next_retry_at/last_error 簿记，
 *    退避门内不可认领，达到 maxAttempts 转死信（status=1）且永不再认领；
 * ③ fan-out 原子性：监听器在排空事务内经集成 Outbox 捕获 N 个集成事件，
 *    与「领域行标记完成」同提交。
 *
 * <p>排空引擎在测试内直接构造（{@code new OutboxRelay(...)}），以控制派发行为与
 * maxAttempts；时钟用 {@link MutableClock}，退避 / 死信判定完全确定。
 */
@SpringBootTest(classes = OutboxRelayTest.TestConfig.class)
@ActiveProfiles("test")
@DisplayName("OutboxRelay —— 排空引擎：派发/幂等重投/退避重试/死信/fan-out 原子性")
class OutboxRelayTest {

    @Configuration
    @SpringBootApplication
    static class TestConfig {
        @Bean
        TestEventCapture testEventCapture() {
            return new TestEventCapture();
        }

        @Bean
        FanOutListener fanOutListener(IntegrationEventOutboxStore integrationEventOutboxStore) {
            return new FanOutListener(integrationEventOutboxStore);
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

    /** fan-out 监听器：OrderPlacedEvent → 3 个集成事件，经集成 Outbox 在排空事务内捕获 */
    static class FanOutListener {
        private final IntegrationEventOutboxStore store;

        FanOutListener(IntegrationEventOutboxStore store) {
            this.store = store;
        }

        @EventListener
        public void onOrderPlaced(OrderPlacedEvent event) {
            store.appendAll(event, List.of(
                    new StockDeductedIntegrationEvent("prod-1", 1),
                    new StockDeductedIntegrationEvent("prod-2", 2),
                    new StockDeductedIntegrationEvent("prod-3", 3)));
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

    private final DomainEventOutboxStore domainEventOutboxStore;
    private final DataSource dataSource;
    private final PlatformTransactionManager transactionManager;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final DomainEventCodec codec;
    private final TestEventCapture eventCapture;
    private final TransactionTemplate txTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final MutableClock clock = new MutableClock();

    @Autowired
    OutboxRelayTest(DomainEventOutboxStore domainEventOutboxStore,
                    DataSource dataSource,
                    PlatformTransactionManager transactionManager,
                    ApplicationEventPublisher applicationEventPublisher,
                    DomainEventCodec codec,
                    TestEventCapture eventCapture) {
        this.domainEventOutboxStore = domainEventOutboxStore;
        this.dataSource = dataSource;
        this.transactionManager = transactionManager;
        this.applicationEventPublisher = applicationEventPublisher;
        this.codec = codec;
        this.eventCapture = eventCapture;
        this.txTemplate = new TransactionTemplate(transactionManager);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM ddd_domain_event_outbox");
        jdbcTemplate.execute("DELETE FROM ddd_integration_event_outbox");
        eventCapture.captured.clear();
    }

    /**
     * 测试后同样清空：多个 @SpringBootTest 上下文共享同一 H2 内存库（testdb），
     * 且排空调度在上下文启动时立即首轮触发——残留行（含 fan-out 待投集成行）
     * 会被下一个上下文的排空器认领，污染其断言。
     */
    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM ddd_domain_event_outbox");
        jdbcTemplate.execute("DELETE FROM ddd_integration_event_outbox");
    }

    // ==================== 成功路径 + 幂等 ====================

    @Test
    @DisplayName("成功路径：drain 派发事件（身份经行重建），同事务标记完成；重排空幂等为 0")
    void drain_happyPath_dispatchesAndMarksDone_thenIdempotent() {
        OrderCancelledEvent event = capture(new OrderCancelledEvent(UUID.randomUUID(), "changed mind"));
        OutboxRelay relay = domainRelay(publishingDispatcher(), 3);

        int processed = relay.drain(10);

        assertThat(processed).isEqualTo(1);
        // 事件在排空时才派发；身份经 codec 以 outbox 行重建（eventId 跨重投稳定）
        assertThat(eventCapture.captured).hasSize(1);
        DomainEvent dispatched = eventCapture.captured.get(0);
        assertThat(dispatched).isInstanceOf(OrderCancelledEvent.class);
        assertThat(dispatched.getEventId()).isEqualTo(event.getEventId());
        assertThat(((OrderCancelledEvent) dispatched).getOrderId()).isEqualTo(event.getOrderId());
        // occurred_on 经 TIMESTAMP 列往返（H2 微秒精度），允许截断误差
        assertThat(dispatched.getOccurredOn())
                .isCloseTo(event.getOccurredOn(), within(1, ChronoUnit.MILLIS));
        // 行已软删（派发副作用与标记完成同事务提交）
        assertThat(domainRowIsDelete(event.getEventId().toString())).isTrue();

        // 幂等重投：已完成行不再被认领
        assertThat(relay.drain(10)).isZero();
        assertThat(eventCapture.captured).hasSize(1);
    }

    // ==================== 失败 → 退避重试 → 死信 ====================

    @Test
    @DisplayName("失败路径：派发异常回滚不标记完成，退避门内不可认领，达上限转死信后永不再认领")
    void drain_dispatchFailure_retriesWithBackoffThenDeadLetters() {
        StockDeductedEvent event = capture(new StockDeductedEvent(1L, 10));
        String rowId = event.getEventId().toString();
        OutboxRelay relay = domainRelay(row -> {
            throw new IllegalStateException("boom: MQ unavailable");
        }, 2);

        // 第一次尝试：派发失败 → 排空事务回滚（未标记完成）→ 独立事务记失败簿记
        assertThat(relay.drain(10)).isEqualTo(1);
        OutboxRowView row = queryDomainRow(rowId);
        assertThat(row.isDelete).isFalse();                       // 未完成
        assertThat(row.attempts).isEqualTo(1);
        assertThat(row.status).isZero();                          // 仍 PENDING
        assertThat(row.nextRetryAt).isNotNull();                  // 指数退避：now + 2s
        assertThat(row.nextRetryAt.toInstant()).isEqualTo(clock.instant().plusSeconds(2));
        assertThat(row.lastError).contains("boom: MQ unavailable");
        assertThat(eventCapture.captured).isEmpty();              // 派发未成功到达监听器

        // 退避窗口内（时钟未推进）：行不可认领
        assertThat(relay.drain(10)).isZero();

        // 推进时钟越过退避点 → 第二次尝试失败 → attempts 达上限 → 转死信
        clock.advance(Duration.ofSeconds(3));
        assertThat(relay.drain(10)).isEqualTo(1);
        row = queryDomainRow(rowId);
        assertThat(row.attempts).isEqualTo(2);
        assertThat(row.status).isEqualTo(1);                      // DEAD
        assertThat(row.isDelete).isFalse();                       // 死信不软删
        assertThat(row.lastError).contains("boom: MQ unavailable");

        // 死信行永不再认领
        clock.advance(Duration.ofDays(1));
        assertThat(relay.drain(10)).isZero();
        assertThat(eventCapture.captured).isEmpty();
    }

    // ==================== fan-out 原子性 ====================

    @Test
    @DisplayName("fan-out 原子性：监听器捕获的集成事件与领域行标记完成同事务落库")
    void drain_fanOut_integrationRowsCapturedAtomicallyWithDomainRow() {
        OrderPlacedEvent event = capture(new OrderPlacedEvent(UUID.randomUUID(), BigDecimal.TEN));
        OutboxRelay relay = domainRelay(publishingDispatcher(), 3);

        assertThat(relay.drain(10)).isEqualTo(1);

        // 领域行已完成
        assertThat(domainRowIsDelete(event.getEventId().toString())).isTrue();
        assertThat(eventCapture.captured).hasSize(1);
        // FanOutListener 在同一排空事务内捕获 3 个集成事件（与标记完成原子提交）
        List<IntegrationRowView> integrationRows = queryIntegrationRows();
        assertThat(integrationRows).hasSize(3);
        assertThat(integrationRows).allSatisfy(row -> {
            assertThat(row.sourceEventId).isEqualTo(event.getEventId().toString());
            assertThat(row.isDelete).isFalse();                   // 待集成排空器投递
            assertThat(row.status).isZero();
        });
    }

    // ==================== helpers ====================

    /** 领域排空引擎（测试内直接构造，控制派发行为与重试上限） */
    private OutboxRelay domainRelay(OutboxRelay.RowDispatcher dispatcher, int maxAttempts) {
        return new OutboxRelay(dataSource, transactionManager, "ddd_domain_event_outbox",
                dispatcher, maxAttempts, Duration.ofMinutes(5), 7, clock);
    }

    /** 与 OutboxAutoConfiguration 领域派发器同形：codec 重建事件 → 进程内发布 */
    private OutboxRelay.RowDispatcher publishingDispatcher() {
        return row -> applicationEventPublisher.publishEvent(
                codec.read(row.eventType(), row.payload(),
                        UUID.fromString(row.id()), row.occurredOn()));
    }

    /** 经真实领域 Outbox 存储在事务内捕获一个事件（提交） */
    private <T extends DomainEvent> T capture(T event) {
        txTemplate.executeWithoutResult(status -> domainEventOutboxStore.appendAll(List.of(event)));
        return event;
    }

    private boolean domainRowIsDelete(String id) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT is_delete FROM ddd_domain_event_outbox WHERE id = ?", Boolean.class, id));
    }

    private OutboxRowView queryDomainRow(String id) {
        return jdbcTemplate.queryForObject(
                "SELECT attempts, status, next_retry_at, last_error, is_delete"
                        + " FROM ddd_domain_event_outbox WHERE id = ?",
                (rs, n) -> {
                    OutboxRowView view = new OutboxRowView();
                    view.attempts = rs.getInt("attempts");
                    view.status = rs.getInt("status");
                    view.nextRetryAt = rs.getTimestamp("next_retry_at");
                    view.lastError = rs.getString("last_error");
                    view.isDelete = rs.getBoolean("is_delete");
                    return view;
                }, id);
    }

    private List<IntegrationRowView> queryIntegrationRows() {
        return jdbcTemplate.query(
                "SELECT source_event_id, status, is_delete FROM ddd_integration_event_outbox",
                (rs, n) -> {
                    IntegrationRowView view = new IntegrationRowView();
                    view.sourceEventId = rs.getString("source_event_id");
                    view.status = rs.getInt("status");
                    view.isDelete = rs.getBoolean("is_delete");
                    return view;
                });
    }

    private static final class OutboxRowView {
        int attempts;
        int status;
        Timestamp nextRetryAt;
        String lastError;
        boolean isDelete;
    }

    private static final class IntegrationRowView {
        String sourceEventId;
        int status;
        boolean isDelete;
    }
}
