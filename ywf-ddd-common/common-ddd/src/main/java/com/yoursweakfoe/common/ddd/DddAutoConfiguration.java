package com.yoursweakfoe.common.ddd;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yoursweakfoe.common.ddd.infrastructure.event.SpringDomainEventPublisher;
import com.yoursweakfoe.common.ddd.infrastructure.mybatis.config.AuditProperties;
import com.yoursweakfoe.common.ddd.infrastructure.mybatis.config.MybatisPlusPluginConfiguration;
import com.yoursweakfoe.common.ddd.infrastructure.mybatis.handler.BasicAutoFillHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * DDD 框架模块自动配置。
 *
 * <p>通过 {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports} 向
 * Spring Boot 3 的 AutoConfiguration 机制注册。业务侧无需显式扫描框架包即可装配以下 Bean：
 *
 * <ul>
 *   <li>{@link SpringDomainEventPublisher} —— 领域事件发布（桥接 Spring ApplicationEventPublisher）
 *   <li>{@link MybatisPlusPluginConfiguration} —— MyBatis-Plus 拦截器（分页 + 乐观锁 + 防全表攻击）
 *   <li>{@link BasicAutoFillHandler} —— createAt / updateAt 自动填充
 * </ul>
 *
 * <p>仅在 MyBatis-Plus（{@link BaseMapper}）存在于 classpath 时激活，
 * 避免纯领域层消费方（仅使用 AggregateRoot / Repository 接口）被强制要求 MyBatis-Plus 运行时。
 */
@AutoConfiguration(after = MybatisPlusAutoConfiguration.class)
@ConditionalOnClass(BaseMapper.class)
@EnableConfigurationProperties(AuditProperties.class)
@Import({
    SpringDomainEventPublisher.class,
    BasicAutoFillHandler.class,
    MybatisPlusPluginConfiguration.class
})
public class DddAutoConfiguration {}
