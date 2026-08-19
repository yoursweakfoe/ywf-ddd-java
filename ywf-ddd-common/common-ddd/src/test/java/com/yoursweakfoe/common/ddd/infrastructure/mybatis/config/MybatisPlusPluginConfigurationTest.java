package com.yoursweakfoe.common.ddd.infrastructure.mybatis.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class MybatisPlusPluginConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MybatisPlusPluginConfiguration.class));

    @Test
    void interceptor_hasThreeInnerInterceptors() {
        contextRunner.run(context -> {
            MybatisPlusInterceptor interceptor = context.getBean(MybatisPlusInterceptor.class);
            assertThat(interceptor.getInterceptors()).hasSize(3);
        });
    }

    @Test
    void firstIsPagination_secondIsOptimisticLocker_thirdIsBlockAttack() {
        contextRunner.run(context -> {
            MybatisPlusInterceptor interceptor = context.getBean(MybatisPlusInterceptor.class);
            var interceptors = interceptor.getInterceptors();
            assertThat(interceptors.get(0)).isInstanceOf(PaginationInnerInterceptor.class);
            assertThat(interceptors.get(1)).isInstanceOf(OptimisticLockerInnerInterceptor.class);
            assertThat(interceptors.get(2)).isInstanceOf(BlockAttackInnerInterceptor.class);
        });
    }

    @Test
    void conditionalOnMissingBean_respected() {
        contextRunner
                .withUserConfiguration(CustomInterceptorConfig.class)
                .run(context -> {
                    MybatisPlusInterceptor interceptor = context.getBean(MybatisPlusInterceptor.class);
                    // Custom interceptor has 0 inner interceptors
                    assertThat(interceptor.getInterceptors()).isEmpty();
                });
    }

    @Configuration
    static class CustomInterceptorConfig {
        @Bean
        MybatisPlusInterceptor mybatisPlusInterceptor() {
            return new MybatisPlusInterceptor();
        }
    }
}
