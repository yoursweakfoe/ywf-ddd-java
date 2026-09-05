package com.yoursweakfoe.common.ddd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.yoursweakfoe.common.ddd.infrastructure.mybatis.config.AuditProperties;
import com.yoursweakfoe.common.ddd.infrastructure.mybatis.handler.AuditFieldFiller;
import java.time.Clock;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * MybatisDddAutoConfiguration 门控测试 —— 装配自检双卫兵的正反两面（audit C7②）：
 * <ul>
 *   <li>正向：容器中存在 SqlSessionFactory（MyBatis 命运栈在位）⇒ 审计组件装配；</li>
 *   <li>负向：容器中没有 SqlSessionFactory（消费方排除 MybatisAutoConfiguration、
 *       或自备 ORM 等场景）⇒ 本配置优雅退化，不产半残 Bean、不炸上下文。</li>
 * </ul>
 * OnClass（classpath 级）与 OnBean（容器级）两卫兵中，前者在测试类路径恒真、
 * 无可观测差异；行为分野由 OnBean 承担，故正反两例均以 Bean 的在缺席为轴。
 */
@DisplayName("MybatisDddAutoConfiguration — 装配自检卫兵")
class MybatisDddAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ClockAutoConfiguration.class,
                    MybatisDddAutoConfiguration.class));

    @Test
    @DisplayName("容器存在 SqlSessionFactory → 审计组件装配（OnBean 卫兵放行）")
    void withMybatisOnClasspath_dddBeansLoaded() {
        // AuditFieldFiller 构造需要 Clock + AuditProperties——分别由 ClockAutoConfiguration
        // 与本配置 @EnableConfigurationProperties 提供（时间源独立于 MyBatis 门控）。
        // SqlSessionFactory 以 mock 充当「MyBatis 栈在位」的容器事实。
        contextRunner.withBean(SqlSessionFactory.class, () -> mock(SqlSessionFactory.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(AuditFieldFiller.class);
                    assertThat(context).hasSingleBean(Clock.class);
                    assertThat(context).hasSingleBean(AuditProperties.class);
                });
    }

    @Test
    @DisplayName("容器无 SqlSessionFactory → 本配置整体退位，不产半残 Bean（负向门控）")
    void withoutSqlSessionFactory_backsOffEntirely() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(AuditFieldFiller.class);
            assertThat(context).hasNotFailed();
            // 时间源独立于 MyBatis 门控：无 SQL 栈也应提供 Clock
            assertThat(context).hasSingleBean(Clock.class);
        });
    }
}
