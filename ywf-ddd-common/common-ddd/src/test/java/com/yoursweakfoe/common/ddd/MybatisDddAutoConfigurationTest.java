package com.yoursweakfoe.common.ddd;

import static org.assertj.core.api.Assertions.assertThat;

import com.yoursweakfoe.common.ddd.infrastructure.mybatis.config.AuditProperties;
import com.yoursweakfoe.common.ddd.infrastructure.mybatis.handler.AuditFieldFiller;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class MybatisDddAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ClockAutoConfiguration.class,
                    MybatisDddAutoConfiguration.class));

    @Test
    void withMybatisOnClasspath_dddBeansLoaded() {
        // 门控：org.apache.ibatis.session.SqlSessionFactory 在 classpath（mybatis-spring-boot-starter 传递）
        // AuditFieldFiller 构造器需要 Clock + AuditProperties —— 分别由 ClockAutoConfiguration
        // 与本配置的 @EnableConfigurationProperties 提供（时间源独立于 MyBatis 门控）
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AuditFieldFiller.class);
            assertThat(context).hasSingleBean(Clock.class);
            assertThat(context).hasSingleBean(AuditProperties.class);
        });
    }
}
