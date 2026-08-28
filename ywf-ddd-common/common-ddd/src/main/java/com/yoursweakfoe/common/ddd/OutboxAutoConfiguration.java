package com.yoursweakfoe.common.ddd;

import com.yoursweakfoe.common.ddd.domain.event.publisher.DomainEventPublisher;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.DomainEventCodec;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.IntegrationEventSender;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.OutboxEnvelope;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.OutboxProperties;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.scheduler.OutboxKind;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.scheduler.OutboxRelay;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.scheduler.OutboxRelayScheduler;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.scheduler.OutboxRowAccess;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Outbox 自动配置 —— 全链路 Outbox 可靠性规范的装配入口（SPI-only，框架零 SQL）。
 *
 * <p><strong>框架领地（全链路规范定稿）</strong>：领域事件与集成事件强制经 Outbox 可靠投递，
 * 本配置装配——
 * <ul>
 *   <li>{@link DomainEventCodec} —— 载荷编解码 + 身份重建（信封四元组标准；纯序列化能力，不依赖存储）</li>
 *   <li>{@link OutboxRelayScheduler} —— 按行访问 SPI 装配的排空引擎群
 *       （认领即加锁、每行一事务、退避重试、死信），时间驱动排空；
 *       已投递行软删留痕，框架不做清除，历史条目的搬运 / 归档归使用方数据抽取层</li>
 * </ul>
 *
 * <p><strong>SPI-only（框架不提供缺省实现）</strong>：捕获侧实现
 * {@code DomainEventOutboxStore} / {@code IntegrationEventOutboxStore}，排空侧实现
 * {@link OutboxRowAccess}，均由使用方注册（参考实现见 sample-application；标准表结构为
 * 参考约定而非框架强制）。
 *
 * <p><strong>装配门控与逃生门</strong>：
 * <ul>
 *   <li>总开关 {@code ywf.ddd.outbox.enabled=false} → 本配置整体退位；退位后聚合一旦注册事件，
 *       {@code DomainEventOutboxCapture} fail-fast 抛错（要么不用事件，要么带上 Outbox）</li>
 *   <li>排空装配门控 {@code @ConditionalOnBean(OutboxRowAccess + PlatformTransactionManager)}：
 *       使用方注册了行访问（且存在事务管理器）即自动获得排空；否则整体静默跳过</li>
 *   <li>每个行访问各装配一个排空引擎（同类多个实现 = 分表各自独立引擎），按 {@link OutboxKind}
 *       选择派发回调：DOMAIN → {@link DomainEventPublisher} 进程内派发；INTEGRATION →
 *       {@link IntegrationEventSender} 投 MQ——<strong>有集成行访问而无 sender 直接 fail-fast</strong>
 *       （集成事件有入箱无投递 = 捕获后永无排空）</li>
 *   <li>{@code @EnableScheduling} 开启全应用调度（幂等无害）</li>
 * </ul>
 *
 * @see OutboxRowAccess
 * @see OutboxProperties
 */
@AutoConfiguration
@EnableScheduling
@EnableConfigurationProperties(OutboxProperties.class)
// 装配顺序锚点：本配置的 @ConditionalOnBean(OutboxRowAccess/PlatformTransactionManager) 条件
// 在「本配置被处理时」求值——必须排在注册这些 Bean 的自动配置之后，否则条件求值时
// Bean 定义尚不存在，排空装配会被静默跳过（捕获照常、永无排空，可靠性担保整体失效）。
// 缺一不可：自动配置在无 @AutoConfigureAfter 约束时按类名字典序解析，
// com.yoursweakfoe.* < com.baomidou.* < org.springframework.*（'c' < 'o'）——
// 即本配置天然先于 Boot 数据源事务管理器自动配置被处理，PTM 条件必然求值过早，
// 故必须以 name 显式锚定「先于我」的自动配置（common-ddd 对二者均无编译依赖，故用字符串）。
// 注：锚点字符串中的 "jdbc" 是 Spring 自动配置类 FQN 的组成部分（装配顺序元数据），
// 不属于「框架零 SQL」约束所指的数据访问绑定（SQL 文本 / 表名 / JDBC API 使用）——勿据此删除。
@AutoConfigureAfter(name = {
        "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration",
        "com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceAutoConfiguration"
})
@ConditionalOnProperty(prefix = "ywf.ddd.outbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxAutoConfiguration {

    /**
     * 事件编解码器 —— 载荷格式自持（专用 JsonMapper，不随消费方应用级配置漂移），
     * 反序列化时以行身份重建 eventId/occurredOn（幂等键跨重投稳定）。
     */
    @Bean
    @ConditionalOnMissingBean
    public DomainEventCodec domainEventCodec() {
        return new DomainEventCodec();
    }

    /**
     * 排空装配入口 —— 按全部已注册的行访问 SPI 各建一个排空引擎，组装排空调度入口。
     *
     * <p>行访问按「类别 → 实现类名」稳定排序（同类多实现的引擎顺序确定，分表排空行为可复现）。
     * 派发回调按 {@link OutboxKind} 选择；INTEGRATION 行访问必须有 {@code IntegrationEventSender}
     * 承接（有入箱无投递 = 捕获后永无排空，启动即失败）。
     */
    @Bean
    @ConditionalOnBean({OutboxRowAccess.class, PlatformTransactionManager.class})
    public OutboxRelayScheduler outboxRelayScheduler(List<OutboxRowAccess> rowAccesses,
                                                     PlatformTransactionManager transactionManager,
                                                     DomainEventPublisher domainEventPublisher,
                                                     ObjectProvider<IntegrationEventSender> senderProvider,
                                                     DomainEventCodec codec,
                                                     Clock clock,
                                                     OutboxProperties properties) {
        List<OutboxRowAccess> ordered = rowAccesses.stream()
                .sorted(Comparator.comparing(OutboxRowAccess::kind)
                        .thenComparing(access -> access.getClass().getName()))
                .toList();

        OutboxProperties.Relay relay = properties.getRelay();
        List<OutboxRelay> relays = new ArrayList<>(ordered.size());
        for (OutboxRowAccess access : ordered) {
            OutboxRelay.RowDispatcher dispatcher;
            if (access.kind() == OutboxKind.DOMAIN) {
                dispatcher = row -> domainEventPublisher.publish(
                        codec.read(row.eventType(), row.payload(), UUID.fromString(row.id()), row.occurredOn()));
            } else {
                IntegrationEventSender sender = senderProvider.getIfAvailable();
                if (sender == null) {
                    throw new IllegalStateException(
                            "OutboxRowAccess(kind=INTEGRATION) registered but no IntegrationEventSender bean "
                                    + "is available: integration events would be captured but never drained. "
                                    + "Register an IntegrationEventSender or remove the INTEGRATION row access.");
                }
                dispatcher = row -> sender.send(new OutboxEnvelope(
                        UUID.fromString(row.id()), row.eventType(), row.payload(), row.occurredOn()));
            }
            relays.add(new OutboxRelay(access, transactionManager, dispatcher,
                    relay.getMaxAttempts(), relay.getMaxBackoff(), clock));
        }
        return new OutboxRelayScheduler(relays, relay.getBatchSize());
    }
}
