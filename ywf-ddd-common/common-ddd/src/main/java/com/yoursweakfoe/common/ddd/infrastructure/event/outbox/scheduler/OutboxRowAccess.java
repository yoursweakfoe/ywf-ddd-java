package com.yoursweakfoe.common.ddd.infrastructure.event.outbox.scheduler;

import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.OutboxRow;
import java.time.OffsetDateTime;

/**
 * Outbox 行访问 SPI —— 排空引擎（{@link OutboxRelay}）与 outbox 存储之间的接缝，
 * 框架纯策略排空边界的持久化面。
 *
 * <p><strong>框架不提供缺省实现</strong>：使用方按本契约自行实现（参考实现见 sample-application）；
 * 标准 outbox 表结构亦为参考约定而非框架强制。框架侧（排空引擎 / 自动装配 / 退避与死信策略）
 * 零 SQL——行的存储形态、加锁方式、完成标记（软删或状态位）均由实现自持。
 *
 * <p><strong>SPI 刻意不设删除 / 清除能力</strong>：框架只写不清——已投递行以软删留痕
 * （审计留痕），历史条目的搬运 / 归档归使用方数据抽取层按自身节奏处理。本 SPI 永不删除事件行。
 *
 * <p><strong>同事务义务（硬约束）</strong>：本接口的每个方法都必须加入调用方当前事务
 * （绝不自行开启新事务）。排空引擎的事务编排为：认领（{@link #claimOne}）+ 派发 + 标记完成
 * （{@link #markDone}）包裹在同一个 {@code REQUIRES_NEW} 事务内原子提交；失败簿记
 * （{@link #recordFailure}）走另一个独立 {@code REQUIRES_NEW} 事务。实现若脱离调用方事务，
 * 「派发副作用 + 标记完成」的原子性即告失效。
 *
 * <p><strong>方法契约</strong>：
 * <ul>
 *   <li>{@link #claimOne} —— 认领一行待投递的行（PENDING 且 next_retry_at 为 NULL 或 ≤ dueBefore），
 *       尽力按 occurred_on FIFO；<strong>必须多实例并发安全</strong>（如 {@code SKIP LOCKED}
 *       行锁跳过或等价机制），行锁持续到调用方事务结束；无到期行返回 {@code null}</li>
 *   <li>{@link #markDone} —— 标记行投递完成（参考表结构中为软删标记）；对同一行幂等</li>
 *   <li>{@link #recordFailure} —— 原样持久化框架计算的簿记（newAttempts / nextRetryAt / lastError /
 *       dead）：<strong>全部重试策略归框架</strong>，实现只做纯持久化，不做任何策略判断</li>
 * </ul>
 *
 * <p>线程安全要求：实现必须支持多线程并发调用（多个排空实例 / 调度线程可能同时认领）。
 *
 * @see OutboxKind
 * @see OutboxRelay
 */
public interface OutboxRowAccess {

    /** 行类别 —— 决定装配的派发回调（领域进程内 / 集成投 MQ）。 */
    OutboxKind kind();

    /**
     * 认领一行到期待投递的行（必须在调用方当前事务内完成，不自行管理事务）。
     *
     * <p>候选条件：待投递（PENDING）且 next_retry_at 为 NULL（立即到期）或 ≤ {@code dueBefore}；
     * 尽力按 occurred_on FIFO。认领必须多实例并发安全，行锁持续到调用方事务结束。
     *
     * @param dueBefore 到期判断基准时刻
     * @return 认领的行；无到期行时返回 {@code null}
     */
    OutboxRow claimOne(OffsetDateTime dueBefore);

    /**
     * 标记行投递完成（与派发副作用同事务提交；参考表结构中为软删标记）。对同一行幂等。
     *
     * @param id          行主键
     * @param completedAt 完成时刻
     */
    void markDone(String id, OffsetDateTime completedAt);

    /**
     * 记录失败簿记（独立于认领事务；原样持久化框架计算值，实现不做策略判断）。
     *
     * @param id          行主键
     * @param newAttempts 累计尝试次数（框架已 +1）
     * @param nextRetryAt 下次重试时刻（框架按指数退避计算）
     * @param lastError   最近一次失败原因（{@code Throwable#toString()}）
     * @param dead        是否已达重试上限转死信（框架判定）
     * @param now         簿记时刻（审计时间是否落列由实现决定）
     */
    void recordFailure(String id, int newAttempts, OffsetDateTime nextRetryAt,
                       String lastError, boolean dead, OffsetDateTime now);
}
