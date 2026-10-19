package com.yoursweakfoe.common.ddd;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * 框架时间源自动配置测试 —— 钉死两条契约：
 * ① 缺省提供 UTC 时钟（与部署环境时区解耦）；
 * ② 业务自定义 Clock Bean 时框架 Bean 退位（方法级 {@code @ConditionalOnMissingBean}）。
 */
@DisplayName("ClockAutoConfiguration —— 框架级时间源")
class ClockAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ClockAutoConfiguration.class));

    @Test
    @DisplayName("缺省提供 UTC 时钟")
    void providesUtcClockByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(Clock.class);
            assertThat(context.getBean(Clock.class).getZone()).isEqualTo(ZoneOffset.UTC);
        });
    }

    @Test
    @DisplayName("业务自定义 Clock Bean → 框架缺省退位（如集成测试注入固定时钟）")
    void consumerClockBean_backsOffFrameworkDefault() {
        Clock fixed = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        contextRunner.withBean(Clock.class, () -> fixed)
                .run(context -> assertThat(context.getBean(Clock.class)).isSameAs(fixed));
    }

    /**
     * 方法级条件的区分性负证：用户 Clock 用非 UTC 时区时，上下文仍恰一枚 Clock、
     * 且为用时区者——证明框架的 UTC 时钟被方法级条件整体跳过（而非与用户 Bean 共存后靠顺序取胜）。
     */
    @Test
    @DisplayName("用户 Clock（非 UTC）在场 → 全上下文仅此一 Clock，框架 UTC 缺省不实例化")
    void userNonUtcClock_isTheOnlyClock() {
        Clock shanghai = Clock.system(java.time.ZoneId.of("Asia/Shanghai"));
        contextRunner.withBean("customClock", Clock.class, () -> shanghai)
                .run(context -> {
                    assertThat(context).hasSingleBean(Clock.class);
                    assertThat(context.getBean(Clock.class).getZone())
                            .isEqualTo(java.time.ZoneId.of("Asia/Shanghai"));
                });
    }
}
