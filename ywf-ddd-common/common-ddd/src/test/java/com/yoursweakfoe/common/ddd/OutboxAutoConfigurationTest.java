package com.yoursweakfoe.common.ddd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.yoursweakfoe.common.ddd.domain.event.publisher.DomainEventPublisher;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.DomainEventCodec;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.IntegrationEventSender;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.OutboxRow;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.scheduler.OutboxKind;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.scheduler.OutboxRelayScheduler;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.scheduler.OutboxRowAccess;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Outbox 自动配置测试 —— 钉死 SPI-only 排空装配契约：
 * ① 无 {@code OutboxRowAccess}：只给 codec（排空整体退位）；
 * ② DOMAIN 行访问 + 事务管理器 → 装配排空调度器；缺事务管理器 → 静默跳过；
 * ③ INTEGRATION 行访问无 {@code IntegrationEventSender} → 启动 fail-fast（有入箱无投递）；
 *    有 sender → 正常装配；
 * ④ 属性开关整体退位。
 *
 * <p>测试统一把排空周期推到极远，避免装配断言期间触发真实调度（副作用隔离）。
 */
@DisplayName("OutboxAutoConfiguration —— SPI-only 排空装配契约")
class OutboxAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OutboxAutoConfiguration.class))
            .withPropertyValues(
                    "ywf.ddd.outbox.relay.fixed-delay=999999999");

    /** 装配断言用最小行访问：装配期只读 {@code kind()}，行方法不参与 */
    static final class StubRowAccess implements OutboxRowAccess {
        private final OutboxKind kind;

        StubRowAccess(OutboxKind kind) {
            this.kind = kind;
        }

        @Override
        public OutboxKind kind() {
            return kind;
        }

        @Override
        public OutboxRow claimOne(OffsetDateTime dueBefore) {
            return null; // 无行可认领：装配断言期间触发的首轮排空为空操作
        }

        @Override
        public void markDone(String id, OffsetDateTime completedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void recordFailure(String id, int newAttempts, OffsetDateTime nextRetryAt,
                                  String lastError, boolean dead, OffsetDateTime now) {
            throw new UnsupportedOperationException();
        }
    }

    private ApplicationContextRunner withTx() {
        return contextRunner
                .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
                .withBean(DomainEventPublisher.class, () -> mock(DomainEventPublisher.class))
                .withBean(Clock.class, Clock::systemUTC);
    }

    @Test
    @DisplayName("无 OutboxRowAccess：只装配 codec（排空整体退位）")
    void withoutRowAccess_codecOnly_noScheduler() {
        withTx().run(context -> {
            assertThat(context).hasSingleBean(DomainEventCodec.class);
            assertThat(context).doesNotHaveBean(OutboxRelayScheduler.class);
        });
    }

    @Test
    @DisplayName("DOMAIN 行访问 + 事务管理器：装配排空调度器")
    void domainRowAccess_withTx_schedulerWired() {
        withTx()
                .withBean("domainAccess", OutboxRowAccess.class, () -> new StubRowAccess(OutboxKind.DOMAIN))
                .run(context -> {
                    assertThat(context).hasSingleBean(DomainEventCodec.class);
                    assertThat(context).hasSingleBean(OutboxRelayScheduler.class);
                });
    }

    @Test
    @DisplayName("有行访问但无事务管理器：排空装配静默跳过（codec 照常）")
    void rowAccess_withoutTx_noScheduler() {
        contextRunner
                .withBean(DomainEventPublisher.class, () -> mock(DomainEventPublisher.class))
                .withBean(Clock.class, Clock::systemUTC)
                .withBean("domainAccess", OutboxRowAccess.class, () -> new StubRowAccess(OutboxKind.DOMAIN))
                .run(context -> {
                    assertThat(context).hasSingleBean(DomainEventCodec.class);
                    assertThat(context).doesNotHaveBean(OutboxRelayScheduler.class);
                });
    }

    @Test
    @DisplayName("INTEGRATION 行访问但无 IntegrationEventSender：启动 fail-fast（有入箱无投递）")
    void integrationRowAccess_withoutSender_startupFails() {
        withTx()
                .withBean("integrationAccess", OutboxRowAccess.class,
                        () -> new StubRowAccess(OutboxKind.INTEGRATION))
                .run(context -> {
                    assertThat(context).hasFailed();
                    Throwable failure = context.getStartupFailure();
                    assertThat(failure).hasRootCauseInstanceOf(IllegalStateException.class);
                    Throwable root = failure;
                    while (root.getCause() != null) {
                        root = root.getCause();
                    }
                    assertThat(root)
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("IntegrationEventSender");
                });
    }

    @Test
    @DisplayName("INTEGRATION 行访问 + sender：装配排空调度器")
    void integrationRowAccess_withSender_schedulerWired() {
        withTx()
                .withBean("integrationAccess", OutboxRowAccess.class,
                        () -> new StubRowAccess(OutboxKind.INTEGRATION))
                .withBean(IntegrationEventSender.class, () -> mock(IntegrationEventSender.class))
                .run(context -> assertThat(context).hasSingleBean(OutboxRelayScheduler.class));
    }

    @Test
    @DisplayName("同类多个行访问（分表）：各自独立引擎，调度器正常装配")
    void multipleSameKindAccesses_eachGetsOwnRelay_schedulerWired() {
        withTx()
                .withBean("domainAccess1", OutboxRowAccess.class, () -> new StubRowAccess(OutboxKind.DOMAIN))
                .withBean("domainAccess2", OutboxRowAccess.class, () -> new StubRowAccess(OutboxKind.DOMAIN))
                .run(context -> assertThat(context).hasSingleBean(OutboxRelayScheduler.class));
    }

    @Test
    @DisplayName("ywf.ddd.outbox.enabled=false：装配整体关闭（codec 也不装配）")
    void disabledByProperty_noBeans() {
        withTx()
                .withBean("domainAccess", OutboxRowAccess.class, () -> new StubRowAccess(OutboxKind.DOMAIN))
                .withPropertyValues("ywf.ddd.outbox.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(DomainEventCodec.class);
                    assertThat(context).doesNotHaveBean(OutboxRelayScheduler.class);
                });
    }
}
