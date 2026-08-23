package com.yoursweakfoe.common.ddd;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yoursweakfoe.common.ddd.domain.event.publisher.DomainEventPublisher;
import com.yoursweakfoe.common.ddd.infrastructure.event.domain.InProcessDomainEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class DomainEventAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DomainEventAutoConfiguration.class));

    @Test
    void registersDomainEventPublisher() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(DomainEventPublisher.class);
            assertThat(context).hasSingleBean(InProcessDomainEventPublisher.class);
        });
    }

    @Test
    void publisherExists_evenWithoutMybatisOnClasspath() {
        // 核心回归：事件发布是纯 Spring 能力，剥离 BaseMapper 后仍应注册（不受 MyBatis 门控牵连）
        contextRunner
                .withClassLoader(new FilteredClassLoader(BaseMapper.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(DomainEventPublisher.class);
                    assertThat(context).hasSingleBean(InProcessDomainEventPublisher.class);
                });
    }
}