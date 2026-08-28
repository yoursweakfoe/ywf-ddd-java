package com.yoursweakfoe.common.ddd.infrastructure.event.outbox.scheduler;

import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.OutboxRow;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Outbox 通用排空引擎 —— 全链路 Outbox 可靠性规范的投递心脏（纯 Java 策略骨架，零 SQL）。
 *
 * <p>行访问经 {@link OutboxRowAccess} SPI 注入：本类只承担策略——认领 → 派发 → 标记完成的
 * 事务编排、指数退避、死信判定；行怎么存、怎么加锁、怎么标记完成，全部归 SPI 实现。
 * 领域 / 集成共享同一引擎，仅「派发回调」（{@link RowDispatcher}）不同：
 * 领域实例经 {@code DomainEventPublisher} 进程内派发，集成实例经 {@code IntegrationEventSender} 投 MQ。
 *
 * <p><strong>已投递行以软删留痕</strong>（参考表结构 is_delete=TRUE）供审计与下游搬运；
 * 框架只认领未完成行（is_delete=FALSE AND status=0 由 SPI 的 {@link #claimOne} 语义保证），
 * 留存行不参与认领、不影响排空，框架永不删除事件行（历史条目的搬运 / 归档归使用方数据抽取层）。
 *
 * <h3>每行一个事务（认领即加锁）</h3>
 * <pre>
 * TX1（REQUIRES_NEW，恒取新连接）：
 *   rowAccess.claimOne(now)   ← 认领即加锁（SPI 保证多实例并发安全，锁持续到本事务结束）
 *   （空 → 空闲退避，跳出）
 *   派发（领域：监听器加入本事务；集成：信封投 MQ）
 *   rowAccess.markDone(...)（标记完成）
 *   COMMIT  ← 「派发副作用 + （集成入箱）+ 标记完成」三者原子
 * TX1 失败：
 * TX2（独立 REQUIRES_NEW）：attempts+=1、next_retry_at=指数退避、last_error；
 *   attempts ≥ max 转 DEAD 并 WARN 告警
 * </pre>
 *
 * <p><strong>标记完成规则</strong>：标记完成与派发副作用在<strong>同一事务</strong>提交——
 * 绝不做 afterCommit 回调或独立事务，二者都会制造崩溃窗口、把免费的原子性降级为「靠幂等兜底的重投」。
 *
 * <p><strong>双连接纪律（结构性保证）</strong>：捕获走业务事务绑定连接（见各 OutboxStore）；
 * 排空簿记恒走本引擎自有的 {@code REQUIRES_NEW} 事务——选 {@code REQUIRES_NEW} 而非 {@code REQUIRED}，
 * 使正确性不依赖「恰好无环境事务」（如测试直接调 {@link #drain}）。没有任何组件跨事务持有连接，
 * 提交后不再有框架代码触碰连接——僵尸事务重投环路无容身之处。
 *
 * <p><strong>投递语义</strong>：at-least-once + 尽力 FIFO（失败重试可能乱序）。消费端按身份幂等去重
 * （领域 = eventId，集成 = 信封 messageId）。
 *
 * <p>{@link #drain(int)} 为确定性测试接缝：测试直调之，不 sleep 等轮询。
 *
 * @see OutboxRowAccess
 * @see OutboxRelayScheduler
 */
@Slf4j
public class OutboxRelay {

    /** 派发回调 —— 领域 / 集成实例的差异点。 */
    @FunctionalInterface
    public interface RowDispatcher {
        void dispatch(OutboxRow row);
    }

    private final OutboxRowAccess rowAccess;
    private final TransactionTemplate requiresNewTx;
    private final RowDispatcher dispatcher;
    private final int maxAttempts;
    private final Duration maxBackoff;
    private final Clock clock;

    public OutboxRelay(OutboxRowAccess rowAccess,
                       PlatformTransactionManager transactionManager,
                       RowDispatcher dispatcher,
                       int maxAttempts,
                       Duration maxBackoff,
                       Clock clock) {
        this.rowAccess = rowAccess;
        this.requiresNewTx = new TransactionTemplate(transactionManager);
        this.requiresNewTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.dispatcher = dispatcher;
        this.maxAttempts = maxAttempts;
        this.maxBackoff = maxBackoff;
        this.clock = clock;
    }

    /**
     * 排空至多 {@code batchSize} 行（每行一个事务）。
     *
     * @return 实际处理（成功或已记失败簿记）的行数；认领不到行时提前返回
     */
    public int drain(int batchSize) {
        int processed = 0;
        for (int i = 0; i < batchSize; i++) {
            if (!processOne()) {
                break;
            }
            processed++;
        }
        return processed;
    }

    // ==================== 内部实现 ====================

    /**
     * 处理单行。
     *
     * @return {@code true} 处理了一行（成功或失败已记簿记）；{@code false} 无可认领行
     */
    private boolean processOne() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        String[] claimedId = {null};
        int[] claimedAttempts = {0};
        try {
            Boolean processed = requiresNewTx.execute(status -> {
                OutboxRow row = rowAccess.claimOne(now);
                if (row == null) {
                    return Boolean.FALSE;
                }
                claimedId[0] = row.id();
                claimedAttempts[0] = row.attempts();
                dispatcher.dispatch(row);
                rowAccess.markDone(row.id(), OffsetDateTime.now(clock));
                return Boolean.TRUE;
            });
            return Boolean.TRUE.equals(processed);
        } catch (RuntimeException dispatchFailed) {
            if (claimedId[0] != null) {
                recordFailure(claimedId[0], claimedAttempts[0], dispatchFailed);
                return true;
            }
            // 认领本身失败（存储错误）——向上抛，交由调度循环的空闲退避
            throw dispatchFailed;
        }
    }

    /** 失败簿记（独立事务）：指数退避 + 死信判定；簿记值全部由框架计算，SPI 原样持久化。 */
    private void recordFailure(String id, int attemptsBefore, RuntimeException cause) {
        int newAttempts = attemptsBefore + 1;
        OffsetDateTime nextRetry = OffsetDateTime.now(clock).plus(backoff(newAttempts));
        boolean dead = newAttempts >= maxAttempts;
        if (dead) {
            log.warn("Outbox row {} dead-lettered after {} attempts: {}", id, newAttempts, cause.toString());
        }
        requiresNewTx.executeWithoutResult(status -> rowAccess.recordFailure(
                id, newAttempts, nextRetry, cause.toString(), dead, OffsetDateTime.now(clock)));
    }

    /** 指数退避：{@code min(2^attempts 秒, maxBackoff)}。 */
    private Duration backoff(int attempts) {
        long seconds = 1L << Math.min(attempts, 30);
        Duration d = Duration.ofSeconds(seconds);
        return d.compareTo(maxBackoff) > 0 ? maxBackoff : d;
    }
}
