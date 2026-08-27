package com.yoursweakfoe.common.ddd.infrastructure.event.outbox.scheduler;

import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Outbox 排空调度入口 —— 时间驱动框架排空器。
 *
 * <p>本类是框架管线（非业务定时入口），<strong>不实现</strong> {@code ScheduledAdapter} 标记
 * （该标记定型「业务时间驱动入口经 AppService 驱动用例」，排空器是基础设施自驱、不含业务编排）；
 * {@code R14b} 已相应收紧为仅约束业务服务的 {@code ..scheduler..} 组件，ArchUnit 不反向约束框架。
 *
 * <p>持有 {@code OutboxAutoConfiguration} 装配的全部排空引擎（{@link OutboxRelay}）：
 * 领域实例（排空 {@code ddd_domain_event_outbox}、进程内派发）恒在；集成实例
 * （排空 {@code ddd_integration_event_outbox}、经 {@code IntegrationEventSender} 投 MQ）
 * 仅在存在 {@code IntegrationEventSender} Bean 时装配——故以 {@link List} 持有、逐个驱动，
 * 不硬依赖两者俱在。
 *
 * <p><strong>优雅停机</strong>：每行一个短事务，上下文关闭停掉轮询、在途事务自然完成，无需额外钩子。
 * 测试不 sleep 等轮询——直接注入 {@link OutboxRelay} 调 {@code drain(n)}（确定性接缝）。
 *
 * @see OutboxRelay
 */
public class OutboxRelayScheduler {

    private final List<OutboxRelay> relays;
    private final int batchSize;

    public OutboxRelayScheduler(List<OutboxRelay> relays, int batchSize) {
        this.relays = relays;
        this.batchSize = batchSize;
    }

    /** 周期排空全部 outbox（fixedDelay：上一轮结束后再计时，天然串行不重叠）。 */
    @Scheduled(fixedDelayString = "${ywf.ddd.outbox.relay.fixed-delay:1000}")
    public void drainOutboxes() {
        for (OutboxRelay relay : relays) {
            relay.drain(batchSize);
        }
    }

    /** 每日物理清除已软删且过保留期的行（框架管线无审计诉求）。 */
    @Scheduled(cron = "${ywf.ddd.outbox.relay.purge-cron:0 0 3 * * *}")
    public void purgeOutboxes() {
        for (OutboxRelay relay : relays) {
            relay.purge();
        }
    }
}
