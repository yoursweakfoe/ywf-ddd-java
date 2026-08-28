package com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.event.outbox;

import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.OutboxRow;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.scheduler.OutboxKind;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.scheduler.OutboxRowAccess;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Outbox 行访问参考实现 —— 标准表（{@code ddd_domain_event_outbox} / {@code ddd_integration_event_outbox}）
 * 的 JDBC 排空读写，履行 {@link OutboxRowAccess} SPI 契约（框架 SPI-only 零 SQL，
 * 排空侧 SQL 由本参考实现自持——原框架 OutboxRelay 内嵌 SQL 外移至此）。
 *
 * <p><strong>一个类装配两个 Bean</strong>：构造时绑定 {@link OutboxKind} 与表名，
 * 领域 / 集成各一（{@code new JdbcOutboxRowAccess(OutboxKind.DOMAIN, "ddd_domain_event_outbox", dataSource)}），
 * 分表场景按需再加实例。
 *
 * <p><strong>同事务义务（硬约束）的履行方式</strong>：全部经 {@link JdbcTemplate} 执行，其底层
 * {@code DataSourceUtils} 复用<strong>当前事务绑定的连接</strong>——本实现不自行开启 / 提交事务。
 * 于是：认领（{@link #claimOne}）+ 派发 + 标记完成（{@link #markDone}）加入框架排空引擎的
 * {@code REQUIRES_NEW} 事务原子提交；失败簿记（{@link #recordFailure}）加入引擎的独立簿记事务。
 * 前提同捕获侧：注入的 {@link DataSource} 必须是事务管理器使用的同一 Bean（多数据源场景为
 * 路由数据源本身，勿解包），否则连接绑定键不同、排空簿记脱离引擎事务，原子性担保失效。
 *
 * <p><strong>并发安全</strong>：认领用 {@code FOR UPDATE SKIP LOCKED} 行锁跳过——多实例 /
 * 多调度线程并发认领互不阻塞，被认领行的锁持续到调用方事务结束（提交或回滚）；无到期行返回
 * {@code null}。{@link JdbcTemplate} 线程安全，全部 SQL 文本为不可变字段。
 *
 * <p><strong>纯持久化（无策略）</strong>：{@link #recordFailure} 原样落库框架计算的簿记值
 * （attempts / nextRetryAt / lastError / dead→status），指数退避与死信判定全部归框架；
 * {@link #markDone} 为软删标记，对同一行幂等。行访问 SPI 不设删除能力——框架只写不清，
 * 已投递行软删留痕，历史条目的搬运 / 归档归使用方数据抽取层。
 *
 * @see OutboxRowAccess
 * @see OutboxKind
 */
public class JdbcOutboxRowAccess implements OutboxRowAccess {

    /** 认领一行：待投递 + 到期（next_retry_at 为 NULL 或 ≤ 基准时刻），FIFO，行锁跳过并发 */
    private final String claimSql;
    /** 标记完成：软删留痕（供审计与下游搬运），is_delete=FALSE 条件保证幂等 */
    private final String markDoneSql;
    /** 失败簿记：框架计算值原样落库（attempts / next_retry_at / last_error / status），无策略 */
    private final String recordFailureSql;

    private final OutboxKind kind;
    private final JdbcTemplate jdbcTemplate;

    public JdbcOutboxRowAccess(OutboxKind kind, String tableName, DataSource dataSource) {
        this.kind = kind;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.claimSql = "SELECT id, event_type, payload, occurred_on, attempts FROM " + tableName
                + " WHERE is_delete = FALSE AND status = 0 AND (next_retry_at IS NULL OR next_retry_at <= ?)"
                + " ORDER BY occurred_on LIMIT 1 FOR UPDATE SKIP LOCKED";
        this.markDoneSql = "UPDATE " + tableName
                + " SET is_delete = TRUE, update_at = ? WHERE id = ? AND is_delete = FALSE";
        this.recordFailureSql = "UPDATE " + tableName
                + " SET attempts = ?, next_retry_at = ?, last_error = ?, status = ?, update_at = ?"
                + " WHERE id = ? AND is_delete = FALSE";
    }

    @Override
    public OutboxKind kind() {
        return kind;
    }

    @Override
    public OutboxRow claimOne(OffsetDateTime dueBefore) {
        return jdbcTemplate.query(claimSql, rs -> {
            if (!rs.next()) {
                return null;
            }
            return mapRow(rs);
        }, Timestamp.from(dueBefore.toInstant()));
    }

    @Override
    public void markDone(String id, OffsetDateTime completedAt) {
        jdbcTemplate.update(markDoneSql, Timestamp.from(completedAt.toInstant()), id);
    }

    @Override
    public void recordFailure(String id, int newAttempts, OffsetDateTime nextRetryAt,
                              String lastError, boolean dead, OffsetDateTime now) {
        jdbcTemplate.update(recordFailureSql,
                newAttempts,
                Timestamp.from(nextRetryAt.toInstant()),
                lastError,
                dead ? 1 : 0,
                Timestamp.from(now.toInstant()),
                id);
    }

    // ==================== 内部实现 ====================

    /** 信封行 → {@link OutboxRow}（occurred_on 为 NULL 时回退 {@link Instant#EPOCH}，与原框架实现一致）。 */
    private OutboxRow mapRow(ResultSet rs) throws SQLException {
        Timestamp occurred = rs.getTimestamp("occurred_on");
        return new OutboxRow(
                rs.getString("id"),
                rs.getString("event_type"),
                rs.getString("payload"),
                occurred != null ? occurred.toInstant() : Instant.EPOCH,
                rs.getInt("attempts"));
    }
}
