package com.yoursweakfoe.common.ddd;

import java.time.Clock;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 框架级时间源自动配置 —— 提供全应用统一的 {@link java.time.Clock} Bean。
 *
 * <p><strong>为什么独立于其它自动配置</strong>：时间源是平台级横切关注点，
 * 与 MyBatis（{@code MybatisDddAutoConfiguration}）等具体能力无归属关系——任何组件
 * （审计填充器、调度器等）都可注入同一时间源。
 *
 * <h3>缺省策略：硬编码 UTC</h3>
 * <p>与企业「存储/运算一律 UTC、渲染本地化交给前端」的实践对齐，且与部署环境的
 * JVM/容器时区（{@code TZ}/{@code -Duser.timezone}）彻底解耦——审计数据不随部署
 * 基线漂移。Spring Boot 官方不提供全局 Clock 自动装配（spring-boot#31397 declined），
 * 故由本框架代管缺省；时区策略属基础设施决策（平台层以 {@code TZ=UTC} 统一），
 * <strong>不设应用级配置属性</strong>以免各服务各自为政。
 *
 * <h3>覆盖方式</h3>
 * <p>业务定义自己的 {@code Clock} Bean 即可使本配置整体退位
 * （类级 {@code @ConditionalOnMissingBean(Clock.class)}）——典型场景：
 * 集成测试注册 {@code Clock.fixed(...)} 使时间断言确定化。这已是 Spring 原生语义
 * （Bean 即配置），无需额外属性命名空间。
 *
 * <p>消费方注入示例：{@code AuditFieldFiller}（经构造器注入）。
 */
@AutoConfiguration
@ConditionalOnMissingBean(Clock.class)
public class ClockAutoConfiguration {

    /** 框架统一时间源 —— 固定 UTC 时区 */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
