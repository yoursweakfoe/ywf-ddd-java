package com.yoursweakfoe.common.ddd;

import com.yoursweakfoe.common.ddd.infrastructure.mybatis.config.AuditProperties;
import com.yoursweakfoe.common.ddd.infrastructure.mybatis.handler.AuditFieldFiller;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * DDD MyBatis 自动配置。
 *
 * <p>通过 {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports} 向
 * Spring Boot 的 AutoConfiguration 机制注册。业务侧无需显式扫描框架包即可装配以下 Bean：
 *
 * <ul>
 *   <li>{@link AuditProperties} —— 审计字段名配置（{@code ywf.ddd.audit.*}）</li>
 *   <li>{@link AuditFieldFiller} —— createAt / updateAt / createdBy / updatedBy 填充器
 *       （由 {@code MybatisPersistence} 在写库前显式调用）</li>
 * </ul>
 *
 * <p>仅在 MyBatis（{@link SqlSessionFactory}）存在于 classpath 时激活，
 * 避免纯领域层消费方（仅使用 AggregateRoot / Repository 接口）被强制要求 MyBatis 运行时。
 *
 * <p><strong>时间源不在此配置</strong>——{@code java.time.Clock} 是平台级横切关注点，
 * 由独立的 {@link ClockAutoConfiguration} 提供（硬编码 UTC 缺省，业务自定义 Clock Bean 时退位）；
 * {@link AuditFieldFiller} 经构造器注入该时间源。
 *
 * <p><strong>无插件栈</strong>——乐观锁版本条件与逻辑删除过滤均由每聚合手写 XML 的 SQL 文本
 * 自身承担（契约见 {@code DddMapper}），框架不注册任何 MyBatis Interceptor。
 */
@AutoConfiguration(after = MybatisAutoConfiguration.class)
@ConditionalOnClass(SqlSessionFactory.class)
@EnableConfigurationProperties(AuditProperties.class)
@Import(AuditFieldFiller.class)
public class MybatisDddAutoConfiguration {}
