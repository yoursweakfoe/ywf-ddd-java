package com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.event.outbox;

import com.yoursweakfoe.common.ddd.application.event.outbox.IntegrationEventOutboxStore;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.DomainEventCodec;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.DomainEventOutboxStore;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.scheduler.OutboxKind;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.scheduler.OutboxRowAccess;
import java.time.Clock;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Outbox 参考实现装配 —— 框架 SPI-only（零 SQL、不提供缺省实现），捕获侧两 Store 与
 * 排空侧两行访问均由使用方注册，本配置即样例工程的参考写法。
 *
 * <p>注册四 Bean：
 * <ul>
 *   <li>{@link DomainEventOutboxStore} —— 领域事件捕获（业务事务内入箱，
 *       {@link JdbcDomainEventOutboxStore}）；</li>
 *   <li>{@link IntegrationEventOutboxStore} —— 集成事件捕获（排空事务内入箱，
 *       {@link JdbcIntegrationEventOutboxStore}）；</li>
 *   <li>DOMAIN {@link OutboxRowAccess} / INTEGRATION {@link OutboxRowAccess} —— 排空侧行访问
 *       （一个 {@link JdbcOutboxRowAccess} 类按类别 + 表名参数化出两实例）。</li>
 * </ul>
 *
 * <p>行访问一经注册，框架 {@code OutboxAutoConfiguration} 即按其装配排空引擎群
 * （DOMAIN → 进程内派发；INTEGRATION → 经 {@code IntegrationEventSender} 投 MQ，
 * 本样例以 {@code LoggingIntegrationEventSender} 日志占位接入）。
 *
 * <p>注入的 {@link DataSource} 必须是事务管理器使用的同一 Bean（同事务捕获 / 同事务簿记的共同前提，
 * 见各实现类 Javadoc）；{@link Clock} 由框架 {@code ClockAutoConfiguration} 缺省提供，
 * {@link DomainEventCodec} 由框架 {@code OutboxAutoConfiguration} 缺省提供。
 */
@Configuration
public class OutboxReferenceConfig {

    @Bean
    public DomainEventOutboxStore domainEventOutboxStore(DataSource dataSource,
                                                         DomainEventCodec codec,
                                                         Clock clock) {
        return new JdbcDomainEventOutboxStore(dataSource, codec, clock);
    }

    @Bean
    public IntegrationEventOutboxStore integrationEventOutboxStore(DataSource dataSource, Clock clock) {
        return new JdbcIntegrationEventOutboxStore(dataSource, clock);
    }

    @Bean
    public OutboxRowAccess domainOutboxRowAccess(DataSource dataSource) {
        return new JdbcOutboxRowAccess(OutboxKind.DOMAIN, "ddd_domain_event_outbox", dataSource);
    }

    @Bean
    public OutboxRowAccess integrationOutboxRowAccess(DataSource dataSource) {
        return new JdbcOutboxRowAccess(OutboxKind.INTEGRATION, "ddd_integration_event_outbox", dataSource);
    }
}
