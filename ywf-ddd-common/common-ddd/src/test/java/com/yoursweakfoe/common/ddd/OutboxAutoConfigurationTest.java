package com.yoursweakfoe.common.ddd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.yoursweakfoe.common.ddd.application.event.outbox.IntegrationEventOutboxStore;
import com.yoursweakfoe.common.ddd.domain.event.publisher.DomainEventPublisher;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.DomainEventCodec;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.DomainEventOutboxStore;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.IntegrationEventSender;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.scheduler.OutboxRelayScheduler;
import java.time.Clock;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Outbox 自动配置测试 —— 钉死全链路 Outbox 装配契约：
 * ① 有 {@code DataSource}：codec + 领域/集成捕获 store + 领域排空引擎 + 调度器；
 * ② 集成排空引擎仅在存在 {@code IntegrationEventSender} 时装配；
 * ③ 无 {@code DataSource}：只给 codec（捕获/排空整体退位）；
 * ④ 属性开关与自定义 Bean 退位。
 *
 * <p>测试统一把排空周期 / 清除计划推到极远，避免装配断言期间触发真实调度（副作用隔离）。
 */
@DisplayName("OutboxAutoConfiguration —— 全链路 Outbox 装配契约")
class OutboxAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OutboxAutoConfiguration.class))
            .withPropertyValues(
                    "ywf.ddd.outbox.relay.fixed-delay=999999999",
                    // Spring cron 仅支持 6 段：推到 2 月 29 日（仅闰年存在）≈ 装配断言期间永不触发
                    "ywf.ddd.outbox.relay.purge-cron=0 0 3 29 2 *");

    private ApplicationContextRunner withFullInfra() {
        return contextRunner
                .withBean(DataSource.class, () -> mock(DataSource.class))
                .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
                .withBean(DomainEventPublisher.class, () -> mock(DomainEventPublisher.class))
                .withBean(Clock.class, Clock::systemUTC);
    }

    @Test
    @DisplayName("有 DataSource：codec + 两 store + 领域排空引擎 + 调度器（集成引擎需 sender）")
    void withDataSource_fullPipeline_noIntegrationRelayWithoutSender() {
        withFullInfra().run(context -> {
            assertThat(context).hasSingleBean(DomainEventCodec.class);
            assertThat(context).hasSingleBean(DomainEventOutboxStore.class);
            assertThat(context).hasSingleBean(IntegrationEventOutboxStore.class);
            assertThat(context).hasBean("domainEventOutboxRelay");
            assertThat(context).doesNotHaveBean("integrationEventOutboxRelay");
            assertThat(context).hasSingleBean(OutboxRelayScheduler.class);
        });
    }

    @Test
    @DisplayName("提供 IntegrationEventSender：集成排空引擎一并装配")
    void withSender_integrationRelayWired() {
        withFullInfra()
                .withBean(IntegrationEventSender.class, () -> mock(IntegrationEventSender.class))
                .run(context -> {
                    assertThat(context).hasBean("domainEventOutboxRelay");
                    assertThat(context).hasBean("integrationEventOutboxRelay");
                    assertThat(context).hasSingleBean(OutboxRelayScheduler.class);
                });
    }

    @Test
    @DisplayName("无 DataSource：只装配 codec（捕获/排空整体退位）")
    void withoutDataSource_codecOnly() {
        contextRunner
                .withBean(DomainEventPublisher.class, () -> mock(DomainEventPublisher.class))
                .withBean(Clock.class, Clock::systemUTC)
                .run(context -> {
                    assertThat(context).hasSingleBean(DomainEventCodec.class);
                    assertThat(context).doesNotHaveBean(DomainEventOutboxStore.class);
                    assertThat(context).doesNotHaveBean(IntegrationEventOutboxStore.class);
                    assertThat(context).doesNotHaveBean(OutboxRelayScheduler.class);
                });
    }

    @Test
    @DisplayName("业务自定义捕获 Store：框架缺省实现退位")
    void customStoreBean_backsOffDefault() {
        DomainEventOutboxStore custom = events -> {
        };
        withFullInfra()
                .withBean(DomainEventOutboxStore.class, () -> custom)
                .run(context -> assertThat(context.getBean(DomainEventOutboxStore.class)).isSameAs(custom));
    }

    @Test
    @DisplayName("ywf.ddd.outbox.enabled=false：装配整体关闭")
    void disabledByProperty_noBeans() {
        withFullInfra()
                .withPropertyValues("ywf.ddd.outbox.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(DomainEventCodec.class);
                    assertThat(context).doesNotHaveBean(DomainEventOutboxStore.class);
                    assertThat(context).doesNotHaveBean(OutboxRelayScheduler.class);
                });
    }
}
