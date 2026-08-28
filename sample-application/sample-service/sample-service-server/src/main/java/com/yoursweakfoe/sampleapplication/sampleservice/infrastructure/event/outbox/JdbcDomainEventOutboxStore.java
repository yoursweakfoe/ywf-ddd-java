package com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.event.outbox;

import com.yoursweakfoe.common.ddd.domain.event.domain.DomainEvent;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.DomainEventCodec;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.DomainEventOutboxStore;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 领域事件 Outbox 捕获参考实现（原框架缺省实现外移）—— 标准表
 * {@code ddd_domain_event_outbox} 的 JDBC 写入。框架 SPI-only（零 SQL），实现归使用方；
 * 本类即样例工程给出的参考写法，业务项目可直接照搬或按需替换。
 *
 * <p><strong>同事务捕获（可靠性锚点）</strong>：经 {@link JdbcTemplate} 写入，底层
 * {@code DataSourceUtils} 复用<strong>当前业务事务绑定的连接</strong>——本实现不自行开启事务，
 * 与业务写入同提交 / 同回滚。前提：注入的 {@link DataSource} 必须是事务管理器使用的同一 Bean
 * （多数据源 dynamic-datasource 场景即 {@code DynamicRoutingDataSource} 本身，
 * <strong>切勿解包为裸连接池</strong>，否则连接绑定键不同、捕获悄悄脱离业务事务，可靠性担保整体失效）。
 *
 * <p><strong>载荷可移植</strong>：payload 以字符串绑定写入 {@code TEXT} 列，跨 H2（测试）/
 * PostgreSQL（生产）一致；框架从不查询载荷内部字段。
 *
 * <p>信封四元组（{@link DomainEventCodec}）：{@code id = eventId}、{@code event_type = 类全限定名}、
 * {@code payload = codec.write(event)}、{@code occurred_on}；簿记列（attempts/status/…）置初始值，
 * 排空由框架排空引擎（{@code OutboxRelay}，经 {@code OutboxRowAccess} 行访问 SPI 读写本表）承担。
 * 审计时间（create_at/update_at）取自注入 {@link Clock}。
 *
 * <p>线程安全：{@link JdbcTemplate} 与 {@link DomainEventCodec} 均线程安全。
 *
 * @see DomainEventOutboxStore
 * @see DomainEventCodec
 */
public class JdbcDomainEventOutboxStore implements DomainEventOutboxStore {

    /** 信封 + 簿记初始值 + 标准结构列一次性写入（占位符仅信封 4 + 审计时间 2） */
    private static final String INSERT_SQL = """
            INSERT INTO ddd_domain_event_outbox
                (id, event_type, payload, occurred_on,
                 attempts, next_retry_at, status, last_error,
                 version, create_at, update_at, created_by, updated_by, is_delete)
            VALUES (?, ?, ?, ?, 0, NULL, 0, NULL, 0, ?, ?, NULL, NULL, FALSE)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final DomainEventCodec codec;
    private final Clock clock;

    public JdbcDomainEventOutboxStore(DataSource dataSource, DomainEventCodec codec, Clock clock) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.codec = codec;
        this.clock = clock;
    }

    @Override
    public void appendAll(List<DomainEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        for (DomainEvent event : events) {
            jdbcTemplate.update(INSERT_SQL,
                    event.getEventId().toString(),
                    event.getClass().getName(),
                    codec.write(event),
                    OffsetDateTime.ofInstant(event.getOccurredOn(), ZoneOffset.UTC),
                    now,
                    now);
        }
    }
}
