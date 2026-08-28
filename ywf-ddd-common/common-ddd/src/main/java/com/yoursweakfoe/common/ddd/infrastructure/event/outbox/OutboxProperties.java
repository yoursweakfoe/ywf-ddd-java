package com.yoursweakfoe.common.ddd.infrastructure.event.outbox;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Outbox 配置属性 —— {@code ywf.ddd.outbox.*}。
 *
 * <p>全链路 Outbox 可靠性规范的运行时调节面。排空周期（{@code relay.fixed-delay}）
 * 经 {@code @Scheduled} 占位符直接读取，其余经本属性绑定。
 *
 * <p><strong>总开关</strong>：{@code enabled=false} 时 {@code OutboxAutoConfiguration} 整体退位
 * （不装配 codec / store / relay / scheduler）。注意：退位后聚合一旦注册事件，
 * {@code DomainEventCapture} 将 fail-fast 抛错——要么不用事件，要么开启 Outbox。
 *
 * @see OutboxAutoConfiguration
 */
@Data
@ConfigurationProperties(prefix = "ywf.ddd.outbox")
public class OutboxProperties {

    /** 总开关（默认开启）。关闭后排空/捕获装配整体退位。 */
    private boolean enabled = true;

    /** 排空器参数。 */
    private Relay relay = new Relay();

    @Data
    public static class Relay {
        /** 排空轮询间隔（毫秒），对应 {@code @Scheduled(fixedDelayString)}。 */
        private long fixedDelay = 1000L;
        /** 单轮排空行数上限。 */
        private int batchSize = 50;
        /** 单行最大重试次数，达到后转死信（status=DEAD）。 */
        private int maxAttempts = 10;
        /** 指数退避封顶（{@code min(2^attempts 秒, 本值)}）。 */
        private Duration maxBackoff = Duration.ofMinutes(5);
    }
}
