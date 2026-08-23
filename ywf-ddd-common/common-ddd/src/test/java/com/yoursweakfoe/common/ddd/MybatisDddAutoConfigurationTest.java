package com.yoursweakfoe.common.ddd;

import static org.assertj.core.api.Assertions.assertThat;

import com.yoursweakfoe.common.ddd.infrastructure.mybatisplus.config.MybatisPlusPluginConfiguration;
import com.yoursweakfoe.common.ddd.infrastructure.mybatisplus.handler.BasicAutoFillHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class MybatisDddAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MybatisPlusDddAutoConfiguration.class));

    @Test
    void withMybatisPlusOnClasspath_mybatisBeansLoaded() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(BasicAutoFillHandler.class);
            assertThat(context.getBeanNamesForType(
                    MybatisPlusPluginConfiguration.class)).hasSize(1);
        });
    }
}