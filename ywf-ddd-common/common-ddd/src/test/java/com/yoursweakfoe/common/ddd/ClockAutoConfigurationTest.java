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
 * ② 业务自定义 Clock Bean 时框架整体退位（类级 {@code @ConditionalOnMissingBean}）。
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
}
