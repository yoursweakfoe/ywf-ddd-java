package com.yoursweakfoe.common.ddd.infrastructure.event.outbox;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.yoursweakfoe.common.contract.dto.event.integration.IntegrationEvent;
import com.yoursweakfoe.common.ddd.application.event.outbox.IntegrationEventOutboxStore;
import com.yoursweakfoe.common.ddd.domain.event.domain.DomainEvent;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * 集成事件 Outbox 缺省捕获实现 —— 标准表 {@code ddd_integration_event_outbox} 的 JDBC 写入。
 *
 * <p><strong>行身份 = MQ messageId</strong>：集成事件是 contract 层纯 POJO（无框架身份字段），
 * 行主键在捕获时铸造新 UUID，即后续投递的信封 {@code messageId}——跨重投稳定（它就是行本身），
 * 下游消费端按它幂等去重。载荷不注入身份，序列化纯 POJO 字段。
 *
 * <p><strong>同事务捕获</strong>：经 {@link JdbcTemplate} 写入，复用调用方事务绑定连接——
 * 集成事件与「领域行标记完成」同提交 / 同回滚，关闭 dual-write 窗口。前提同
 * {@link JdbcDomainEventOutboxStore}：注入的 {@link DataSource} 必须是事务管理器使用的同一 Bean。
 *
 * <p><strong>载荷格式自持</strong>：持有专用 {@link JsonMapper}（字段可见性 ANY，按声明字段捕获），
 * 不随消费方应用级序列化配置漂移。{@code source_event_id} 记录源领域事件 eventId（fan-out 血缘），
 * 入站再发出（{@code source == null}）时为 NULL。
 *
 * <p>线程安全：{@link JdbcTemplate} 与 {@link JsonMapper} 均线程安全。
 *
 * @see IntegrationEventOutboxStore
 * @see IntegrationEventSender
 */
public class JdbcIntegrationEventOutboxStore implements IntegrationEventOutboxStore {

    /** 信封 + 溯源 + 簿记初始值 + 标准结构列一次性写入 */
    private static final String INSERT_SQL = """
            INSERT INTO ddd_integration_event_outbox
                (id, event_type, payload, occurred_on, source_event_id,
                 attempts, next_retry_at, status, last_error,
                 version, create_at, update_at, created_by, updated_by, is_delete)
            VALUES (?, ?, ?, ?, ?, 0, NULL, 0, NULL, 0, ?, ?, NULL, NULL, FALSE)
            """;

    /** 自持载荷格式：字段可见性 ANY（完整捕获声明字段，不依赖 getter 惯例） */
    private final JsonMapper mapper = JsonMapper.builder()
            .changeDefaultVisibility(vc -> vc.withFieldVisibility(JsonAutoDetect.Visibility.ANY))
            .build();

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public JdbcIntegrationEventOutboxStore(DataSource dataSource, Clock clock) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.clock = clock;
    }

    @Override
    public void appendAll(DomainEvent source, List<IntegrationEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        String sourceEventId = source != null ? source.getEventId().toString() : null;
        for (IntegrationEvent event : events) {
            jdbcTemplate.update(INSERT_SQL,
                    UUID.randomUUID().toString(),
                    event.getClass().getName(),
                    write(event),
                    OffsetDateTime.ofInstant(now.toInstant(), ZoneOffset.UTC),
                    sourceEventId,
                    now,
                    now);
        }
    }

    /** 序列化集成事件为 JSON 载荷（字段级捕获）。 */
    private String write(IntegrationEvent event) {
        try {
            return mapper.writeValueAsString(event);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Failed to serialize integration event: " + event, e);
        }
    }
}
