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
 * <p>持有 {@code OutboxAutoConfiguration} 按行访问 SPI（{@link OutboxRowAccess}）装配的全部
 * 排空引擎（{@link OutboxRelay}）：每个已注册的行访问各自独立一个引擎——领域（进程内派发）/
 * 集成（经 {@code IntegrationEventSender} 投 MQ）、分表皆然——故以 {@link List} 持有、
 * 逐个驱动，排空各自行访问所辖的 outbox 行集。职责仅排空（认领 → 派发 → 标记完成）；
 * 已投递行软删留痕，框架不做清除，历史条目的搬运 / 归档归使用方数据抽取层。
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
}
