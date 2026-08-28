package com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.event.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.yoursweakfoe.common.ddd.application.event.outbox.IntegrationEventOutboxStore;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.DomainEventOutboxStore;
import com.yoursweakfoe.sampleapplication.sampleservice.Application;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.event.integration.OrderPlacedIntegrationEvent;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.event.domain.OrderPlacedEvent;
import com.yoursweakfoe.sampleapplication.sampleservice.integration.TestSecurityConfiguration;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * JDBC Outbox 捕获存储测试（参考实现随框架 SPI-only 化外移至样例工程）——
 * 钉死「同事务捕获」可靠性锚点：
 * ① 领域存储：捕获行信封四元组（id=eventId / event_type / payload / occurred_on）+ 簿记初始值；
 *    提交则行落库、回滚则行随之消失（与业务写入同提交 / 同回滚）；
 * ② 集成存储：行身份为铸造的新 UUID（= 未来 MQ messageId）、source_event_id 溯源血缘
 *    （source==null 时为 NULL）、载荷按声明字段捕获。
 *
 * <p>全部基于真实 H2（MODE=PostgreSQL）数据源与真实事务（{@link TransactionTemplate}），
 * 行检查用 {@link JdbcTemplate} 直查。
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@Import(TestSecurityConfiguration.class)
@DisplayName("JdbcOutboxStore —— 领域/集成事件同事务捕获（提交落库、回滚消失）")
class JdbcOutboxStoreTest {

    private final DomainEventOutboxStore domainEventOutboxStore;
    private final IntegrationEventOutboxStore integrationEventOutboxStore;
    private final TransactionTemplate txTemplate;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    JdbcOutboxStoreTest(DomainEventOutboxStore domainEventOutboxStore,
                        IntegrationEventOutboxStore integrationEventOutboxStore,
                        PlatformTransactionManager transactionManager,
                        DataSource dataSource) {
        this.domainEventOutboxStore = domainEventOutboxStore;
        this.integrationEventOutboxStore = integrationEventOutboxStore;
        this.txTemplate = new TransactionTemplate(transactionManager);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM ddd_domain_event_outbox");
        jdbcTemplate.execute("DELETE FROM ddd_integration_event_outbox");
    }

    /**
     * 测试后同样清空：多个 @SpringBootTest 上下文共享同一 H2 内存库（testdb），
     * 且排空调度在上下文启动时立即首轮触发——残留待投行会被下一个上下文的排空器认领，
     * 污染其事件捕获断言。
     */
    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM ddd_domain_event_outbox");
        jdbcTemplate.execute("DELETE FROM ddd_integration_event_outbox");
    }

    // ==================== 领域事件存储 ====================

    @Test
    @DisplayName("提交：捕获行落库 —— 信封四元组正确，簿记列为初始值")
    void domainStore_commit_persistsRowWithEnvelopeAndBookkeeping() {
        OrderPlacedEvent event = new OrderPlacedEvent(UUID.randomUUID(), new BigDecimal("59.98"), "customer-1");

        txTemplate.executeWithoutResult(status -> domainEventOutboxStore.appendAll(List.of(event)));

        // 提交后行仍在（「状态已提交 ⇒ 事件必然已落库」的捕获侧证据）
        OutboxRow row = queryDomainRow(event.getEventId().toString());
        assertThat(row).isNotNull();
        // 信封四元组：id = eventId、event_type = 类全限定名、payload = 载荷、occurred_on = 发生时间
        assertThat(row.id).isEqualTo(event.getEventId().toString());
        assertThat(row.eventType).isEqualTo(OrderPlacedEvent.class.getName());
        assertThat(row.payload).contains(event.getOrderId().toString());
        assertThat(row.payload).contains("59.98");
        assertThat(row.occurredOn.toInstant())
                .isCloseTo(event.getOccurredOn(), within(1, ChronoUnit.MILLIS));
        // 簿记初始值：待投递、零重试、无死信原因、未软删、无审计身份
        assertThat(row.attempts).isZero();
        assertThat(row.status).isZero();
        assertThat(row.nextRetryAt).isNull();
        assertThat(row.lastError).isNull();
        assertThat(row.isDelete).isFalse();
        assertThat(row.createdBy).isNull();
        assertThat(row.updatedBy).isNull();
        assertThat(row.createAt).isNotNull();
        assertThat(row.updateAt).isNotNull();
    }

    @Test
    @DisplayName("回滚：事务内可见的捕获行随事务回滚而消失（与业务同回滚）")
    void domainStore_rollback_rowRolledBack() {
        OrderPlacedEvent event = new OrderPlacedEvent(UUID.randomUUID(), BigDecimal.TEN, "customer-1");
        String rowId = event.getEventId().toString();

        txTemplate.executeWithoutResult(status -> {
            domainEventOutboxStore.appendAll(List.of(event));
            // 事务内可见（捕获确实发生在当前事务）
            assertThat(queryDomainRow(rowId)).isNotNull();
            status.setRollbackOnly();
        });

        // 回滚后行消失（不存在「业务回滚但事件残留」的窗口）
        assertThat(queryDomainRow(rowId)).isNull();
    }

    // ==================== 集成事件存储 ====================

    @Test
    @DisplayName("fan-out 捕获：两个集成事件各铸新 UUID 行身份，source_event_id 记录源领域事件")
    void integrationStore_twoEvents_twoRowsWithFreshIdsAndSourceLineage() {
        OrderPlacedEvent source = new OrderPlacedEvent(UUID.randomUUID(), BigDecimal.TEN, "customer-1");
        OrderPlacedIntegrationEvent ie1 = new OrderPlacedIntegrationEvent("order-1", "customer-1");
        OrderPlacedIntegrationEvent ie2 = new OrderPlacedIntegrationEvent("order-2", "customer-2");

        txTemplate.executeWithoutResult(status ->
                integrationEventOutboxStore.appendAll(source, List.of(ie1, ie2)));

        List<OutboxRow> rows = jdbcTemplate.query(
                "SELECT id, event_type, payload, occurred_on, source_event_id, attempts,"
                        + " next_retry_at, status, last_error, is_delete, create_at, update_at,"
                        + " created_by, updated_by FROM ddd_integration_event_outbox",
                this::mapRow);
        assertThat(rows).hasSize(2);
        // 行身份 = 铸造的新 UUID（未来 MQ messageId），与源领域事件身份不同
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.id).isNotEqualTo(source.getEventId().toString());
            UUID.fromString(row.id); // 合法 UUID
            assertThat(row.sourceEventId).isEqualTo(source.getEventId().toString());
            assertThat(row.eventType).isEqualTo(OrderPlacedIntegrationEvent.class.getName());
            assertThat(row.attempts).isZero();
            assertThat(row.status).isZero();
            assertThat(row.isDelete).isFalse();
        });
        assertThat(rows.get(0).id).isNotEqualTo(rows.get(1).id);
        // 载荷按声明字段捕获（纯 POJO 字段级序列化）
        assertThat(rows).extracting(r -> r.payload)
                .anySatisfy(payload -> {
                    assertThat(payload).contains("order-1");
                    assertThat(payload).contains("customer-1");
                })
                .anySatisfy(payload -> {
                    assertThat(payload).contains("order-2");
                    assertThat(payload).contains("customer-2");
                });
    }

    @Test
    @DisplayName("入站再发出（source==null）：source_event_id 为 NULL")
    void integrationStore_nullSource_sourceEventIdIsNull() {
        OrderPlacedIntegrationEvent ie = new OrderPlacedIntegrationEvent("order-9", "customer-9");

        txTemplate.executeWithoutResult(status ->
                integrationEventOutboxStore.appendAll(null, List.of(ie)));

        List<OutboxRow> rows = jdbcTemplate.query(
                "SELECT id, event_type, payload, occurred_on, source_event_id, attempts,"
                        + " next_retry_at, status, last_error, is_delete, create_at, update_at,"
                        + " created_by, updated_by FROM ddd_integration_event_outbox",
                this::mapRow);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).sourceEventId).isNull();
    }

    // ==================== helpers ====================

    /** 直查领域 outbox 行（不存在返回 null） */
    private OutboxRow queryDomainRow(String id) {
        List<OutboxRow> rows = jdbcTemplate.query(
                "SELECT id, event_type, payload, occurred_on, attempts, next_retry_at, status,"
                        + " last_error, is_delete, create_at, update_at, created_by, updated_by"
                        + " FROM ddd_domain_event_outbox WHERE id = ?",
                (rs, n) -> {
                    OutboxRow row = mapSharedColumns(rs);
                    row.sourceEventId = null; // 领域表无溯源列
                    return row;
                }, id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private OutboxRow mapSharedColumns(ResultSet rs) throws SQLException {
        OutboxRow row = new OutboxRow();
        row.id = rs.getString("id");
        row.eventType = rs.getString("event_type");
        row.payload = rs.getString("payload");
        row.occurredOn = rs.getTimestamp("occurred_on");
        row.attempts = rs.getInt("attempts");
        row.nextRetryAt = rs.getTimestamp("next_retry_at");
        row.status = rs.getInt("status");
        row.lastError = rs.getString("last_error");
        row.isDelete = rs.getBoolean("is_delete");
        row.createAt = rs.getTimestamp("create_at");
        row.updateAt = rs.getTimestamp("update_at");
        row.createdBy = rs.getString("created_by");
        row.updatedBy = rs.getString("updated_by");
        return row;
    }

    private OutboxRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        OutboxRow row = mapSharedColumns(rs);
        row.sourceEventId = rs.getString("source_event_id");
        return row;
    }

    /** outbox 行视图（列名经 ResultSet 大小写不敏感解析，H2/PG 通用） */
    private static final class OutboxRow {
        String id;
        String eventType;
        String payload;
        Timestamp occurredOn;
        String sourceEventId;
        int attempts;
        Timestamp nextRetryAt;
        int status;
        String lastError;
        boolean isDelete;
        Timestamp createAt;
        Timestamp updateAt;
        String createdBy;
        String updatedBy;
    }
}
