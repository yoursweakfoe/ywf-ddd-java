package com.yoursweakfoe.common.ddd;

import com.yoursweakfoe.common.ddd.application.event.outbox.IntegrationEventOutboxStore;
import com.yoursweakfoe.common.ddd.domain.event.domain.DomainEvent;
import com.yoursweakfoe.common.ddd.domain.event.publisher.DomainEventPublisher;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.DomainEventCodec;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.DomainEventOutboxStore;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.IntegrationEventSender;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.JdbcDomainEventOutboxStore;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.JdbcIntegrationEventOutboxStore;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.OutboxEnvelope;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.OutboxProperties;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.scheduler.OutboxRelay;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.scheduler.OutboxRelayScheduler;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
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
 * Outbox 自动配置 —— 全链路 Outbox 可靠性规范的装配入口。
 *
 * <p><strong>框架领地（全链路规范定稿）</strong>：领域事件与集成事件强制经 Outbox 可靠投递，
 * 本配置装配完整管线——
 * <ul>
 *   <li>{@link DomainEventCodec} —— 载荷编解码 + 身份重建（信封四元组标准）</li>
 *   <li>{@link JdbcDomainEventOutboxStore} —— 领域事件缺省捕获（{@code ddd_domain_event_outbox}）</li>
 *   <li>{@link JdbcIntegrationEventOutboxStore} —— 集成事件缺省捕获（{@code ddd_integration_event_outbox}）</li>
 *   <li>{@link OutboxRelay} × 2 —— 领域 / 集成排空引擎（认领即加锁、每行一事务、退避重试、死信、保留期清除）</li>
 *   <li>{@link OutboxRelayScheduler} —— 时间驱动排空 + 每日清除</li>
 * </ul>
 *
 * <p><strong>装配门控与逃生门</strong>：
 * <ul>
 *   <li>总开关 {@code ywf.ddd.outbox.enabled=false} → 本配置整体退位；退位后聚合一旦注册事件，
 *       {@code DomainEventFlusher} fail-fast 抛错（要么不用事件，要么开启 Outbox）</li>
 *   <li>捕获 / 排空 Bean 均 {@code @ConditionalOnMissingBean}，业务可整体替换任一环节</li>
 *   <li>store / relay 需 {@code DataSource}；集成 relay 另需 {@code IntegrationEventSender}
 *       （common-mq 未建设，样例以日志占位实现接入）</li>
 *   <li>{@code @EnableScheduling} 开启全应用调度（幂等无害）</li>
 * </ul>
 *
 * <p><strong>连接纪律</strong>：注入的 {@link DataSource} 必须是事务管理器使用的同一 Bean
 * （dynamic-datasource 场景即 {@code DynamicRoutingDataSource} 本身，勿解包裸池）——
 * 否则捕获脱离业务事务，可靠性担保整体失效。
 *
 * @see OutboxProperties
 */
@AutoConfiguration
@EnableScheduling
@EnableConfigurationProperties(OutboxProperties.class)
// 装配顺序锚点：本配置的 @ConditionalOnBean(DataSource/PlatformTransactionManager) 条件
// 在「本配置被处理时」求值——必须排在注册这些 Bean 的自动配置之后，否则条件求值时
// Bean 定义尚不存在，排空引擎会被静默跳过（捕获照常、永无排空，可靠性担保整体失效）。
// name 引用（非常量类引用）：common-ddd 对 spring-boot-jdbc / dynamic-datasource 无编译依赖。
@AutoConfigureAfter(name = {
        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
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

    /** 领域事件缺省捕获（与业务同事务写 {@code ddd_domain_event_outbox}）。 */
    @Bean
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnMissingBean(DomainEventOutboxStore.class)
    public DomainEventOutboxStore domainEventOutboxStore(DataSource dataSource,
                                                        DomainEventCodec codec,
                                                        Clock clock) {
        return new JdbcDomainEventOutboxStore(dataSource, codec, clock);
    }

    /** 集成事件缺省捕获（与调用方事务同写 {@code ddd_integration_event_outbox}）。 */
    @Bean
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnMissingBean(IntegrationEventOutboxStore.class)
    public IntegrationEventOutboxStore integrationEventOutboxStore(DataSource dataSource, Clock clock) {
        return new JdbcIntegrationEventOutboxStore(dataSource, clock);
    }

    /**
     * 领域排空引擎 —— 认领 {@code ddd_domain_event_outbox} 行 → 经 codec 重建事件身份 →
     * {@link DomainEventPublisher} 进程内派发（监听器加入本事务）→ 同事务标记完成。
     */
    @Bean(name = "domainEventOutboxRelay")
    @ConditionalOnBean({DataSource.class, PlatformTransactionManager.class})
    public OutboxRelay domainEventOutboxRelay(DataSource dataSource,
                                              PlatformTransactionManager transactionManager,
                                              DomainEventPublisher domainEventPublisher,
                                              DomainEventCodec codec,
                                              Clock clock,
                                              OutboxProperties properties) {
        OutboxRelay.RowDispatcher dispatcher = row -> {
            DomainEvent event = codec.read(
                    row.eventType(), row.payload(), UUID.fromString(row.id()), row.occurredOn());
            domainEventPublisher.publish(event);
        };
        OutboxProperties.Relay relay = properties.getRelay();
        return new OutboxRelay(dataSource, transactionManager, "ddd_domain_event_outbox", dispatcher,
                relay.getMaxAttempts(), relay.getMaxBackoff(), relay.getRetentionDays(), clock);
    }

    /**
     * 集成排空引擎 —— 认领 {@code ddd_integration_event_outbox} 行 → 构造信封 →
     * {@link IntegrationEventSender} 投 MQ → 同事务标记完成。仅在存在
     * {@code IntegrationEventSender} Bean 时装配（common-mq 未建设时样例以日志占位接入）。
     */
    @Bean(name = "integrationEventOutboxRelay")
    @ConditionalOnBean({DataSource.class, PlatformTransactionManager.class, IntegrationEventSender.class})
    public OutboxRelay integrationEventOutboxRelay(DataSource dataSource,
                                                   PlatformTransactionManager transactionManager,
                                                   IntegrationEventSender sender,
                                                   Clock clock,
                                                   OutboxProperties properties) {
        OutboxRelay.RowDispatcher dispatcher = row -> sender.send(
                new OutboxEnvelope(UUID.fromString(row.id()), row.eventType(), row.payload(), row.occurredOn()));
        OutboxProperties.Relay relay = properties.getRelay();
        return new OutboxRelay(dataSource, transactionManager, "ddd_integration_event_outbox", dispatcher,
                relay.getMaxAttempts(), relay.getMaxBackoff(), relay.getRetentionDays(), clock);
    }

    /** 排空调度入口 —— 驱动全部已装配的排空引擎（领域恒在、集成视 sender 而定）。 */
    @Bean
    @ConditionalOnBean(OutboxRelay.class)
    public OutboxRelayScheduler outboxRelayScheduler(ObjectProvider<OutboxRelay> relays,
                                                     OutboxProperties properties) {
        List<OutboxRelay> relayList = relays.orderedStream().toList();
        return new OutboxRelayScheduler(relayList, properties.getRelay().getBatchSize());
    }
}
