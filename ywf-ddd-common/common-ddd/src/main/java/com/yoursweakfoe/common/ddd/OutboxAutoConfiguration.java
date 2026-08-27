package com.yoursweakfoe.common.ddd;

import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.DomainEventCodec;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Outbox 自动配置 —— 领域事件「同事务捕获」的装配入口。
 *
 * <p><strong>框架领地（收缩后定稿）</strong>：只装配捕获的公共工具——{@link DomainEventCodec}
 * （载荷编解码 + 身份重建，信封四元组标准）。捕获契约 {@code OutboxStore} 本身是纯 SPI，
 * <strong>框架不提供任何缺省实现</strong>：业务侧提供自己的 {@code OutboxStore} Bean 即激活
 * 捕获路径（{@code DomainEventFlusher} 经 {@code ObjectProvider} 自动接入）；未提供时事件
 * 回退直发路径（提交后进程内派发）。表结构参考
 * {@code common-ddd/src/main/resources/sql/ddd_outbox.example.sql}。
 *
 * <p>入箱之后的扫描 / 派发 / 重试 / 死信归业务侧排空器或生态方案
 * （MQ 事务消息 / CDC / Modulith EPR），框架一律不内置。
 *
 * <p>总开关：{@code ywf.ddd.outbox.enabled=false} 时本配置整体退位（不提供 codec，
 * 业务若仍自建捕获需自行装配编解码）。
 *
 * @see DomainEventCodec
 * @see com.yoursweakfoe.common.ddd.infrastructure.event.outbox.OutboxStore
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "ywf.ddd.outbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxAutoConfiguration {

    /**
     * 事件编解码器 —— 载荷格式自持（专用 JsonMapper，不随消费方应用级配置漂移），
     * 反序列化时以行身份重建 eventId/occurredOn（幂等键跨重投稳定）。
     * 业务的 {@code OutboxStore} 实现与排空器共用同一编解码契约。
     */
    @Bean
    @ConditionalOnMissingBean
    public DomainEventCodec domainEventCodec() {
        return new DomainEventCodec();
    }
}
