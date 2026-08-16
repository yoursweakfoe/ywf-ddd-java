package com.yoursweakfoe.common.ddd.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.yoursweakfoe.common.ddd.DddAutoConfiguration;
import com.yoursweakfoe.common.ddd.infrastructure.event.domain.InProcessDomainEventPublisher;
import com.yoursweakfoe.common.ddd.infrastructure.mybatis.config.MybatisPlusPluginConfiguration;
import com.yoursweakfoe.common.ddd.infrastructure.mybatis.handler.BasicAutoFillHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class DddAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DddAutoConfiguration.class));

    @Test
    void withMybatisPlusOnClasspath_allBeansLoaded() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(InProcessDomainEventPublisher.class);
            assertThat(context).hasSingleBean(BasicAutoFillHandler.class);
            assertThat(context.getBeanNamesForType(
                    MybatisPlusPluginConfiguration.class)).hasSize(1);
        });
    }
}
