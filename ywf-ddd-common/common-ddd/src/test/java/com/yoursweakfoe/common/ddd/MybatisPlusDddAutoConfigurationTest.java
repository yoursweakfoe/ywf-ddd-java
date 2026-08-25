package com.yoursweakfoe.common.ddd;

import static org.assertj.core.api.Assertions.assertThat;

import com.yoursweakfoe.common.ddd.infrastructure.mybatisplus.config.MybatisPlusPluginConfiguration;
import com.yoursweakfoe.common.ddd.infrastructure.mybatisplus.handler.BasicAutoFillHandler;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class MybatisPlusDddAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ClockAutoConfiguration.class,
                    MybatisPlusDddAutoConfiguration.class));

    @Test
    void withMybatisPlusOnClasspath_mybatisBeansLoaded() {
        // BasicAutoFillHandler 构造器需要 Clock —— 由 ClockAutoConfiguration 提供（时间源独立于 MP 门控）
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(BasicAutoFillHandler.class);
            assertThat(context).hasSingleBean(Clock.class);
            assertThat(context.getBeanNamesForType(
                    MybatisPlusPluginConfiguration.class)).hasSize(1);
        });
    }
}
