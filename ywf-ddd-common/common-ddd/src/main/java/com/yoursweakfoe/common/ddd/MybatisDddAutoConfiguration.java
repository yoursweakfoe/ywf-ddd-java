package com.yoursweakfoe.common.ddd;

import com.yoursweakfoe.common.ddd.infrastructure.mybatis.config.AuditProperties;
import com.yoursweakfoe.common.ddd.infrastructure.mybatis.handler.AuditFieldFiller;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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
 * <p><strong>装配宣言</strong>：common-ddd 是定型装配（opinionated starter），不是中立库——
 * 引入本包即承诺 Boot + MyBatis + JDBC 持久栈为其使用方的<strong>命运依赖</strong>
 * （所有采用服务都是单 jar 全套四层，不存在纯领域消费方；判据见 .agents/rules/04「Common 模块约束」）。
 *
 * <p>{@code @ConditionalOnClass} + {@code @ConditionalOnBean(SqlSessionFactory)} 的意义是
 * <strong>装配自检双卫兵</strong>而非「保护不存在的纯领域消费方」：
 * classpath 级排除（dependencyManagement 剔 starter）由 OnClass 挡住，
 * 容器级缺席（排除 MybatisAutoConfiguration / 自备 ORM 等场景）由 OnBean 挡住——
 * 两路都优雅退化（不产半残 Bean），无 MyBatis 环境下 bean 装配错误不致炸得难读。
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
@ConditionalOnBean(SqlSessionFactory.class)
@EnableConfigurationProperties(AuditProperties.class)
@Import(AuditFieldFiller.class)
public class MybatisDddAutoConfiguration {}
