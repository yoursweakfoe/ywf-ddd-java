package com.yoursweakfoe.common.ddd;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yoursweakfoe.common.ddd.infrastructure.mybatisplus.config.AuditProperties;
import com.yoursweakfoe.common.ddd.infrastructure.mybatisplus.config.MybatisPlusPluginConfiguration;
import com.yoursweakfoe.common.ddd.infrastructure.mybatisplus.handler.BasicAutoFillHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * DDD MyBatis-Plus 自动配置。
 *
 * <p>通过 {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports} 向
 * Spring Boot 3 的 AutoConfiguration 机制注册。业务侧无需显式扫描框架包即可装配以下 Bean：
 *
 * <ul>
 *   <li>{@link MybatisPlusPluginConfiguration} —— MyBatis-Plus 拦截器（分页 + 乐观锁 + 防全表攻击）</li>
 *   <li>{@link BasicAutoFillHandler} —— createAt / updateAt / createdBy / updatedBy 自动填充</li>
 * </ul>
 *
 * <p>仅在 MyBatis-Plus（{@link BaseMapper}）存在于 classpath 时激活，
 * 避免纯领域层消费方（仅使用 AggregateRoot / Repository 接口）被强制要求 MyBatis-Plus 运行时。
 *
 * <p><strong>时间源不在此配置</strong>——{@code java.time.Clock} 是平台级横切关注点，
 * 由独立的 {@link ClockAutoConfiguration} 提供（硬编码 UTC 缺省，业务自定义 Clock Bean 时退位）；
 * {@link BasicAutoFillHandler} 经构造器注入该时间源。
 */
@AutoConfiguration(after = MybatisPlusAutoConfiguration.class)
@ConditionalOnClass(BaseMapper.class)
@EnableConfigurationProperties(AuditProperties.class)
@Import({
    BasicAutoFillHandler.class,
    MybatisPlusPluginConfiguration.class
})
public class MybatisPlusDddAutoConfiguration {}
