package com.yoursweakfoe.common.ddd.infrastructure.mybatisplus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * DDD MyBatis-Plus 插件配置（record，构造器绑定）。
 *
 * <p>控制框架默认注册的拦截器组合，业务可按需 opt-out。前缀 {@code ywf.ddd.mybatisplus}：
 *
 * <ul>
 *   <li>{@code block-attack-enabled} —— 防全表 UPDATE/DELETE 拦截器开关（默认开启）。
 *       数据修复等确需全表操作的场景可显式关闭；关闭后请改用原生 MyBatis 编写全表 SQL。</li>
 *   <li>{@code pagination-max-limit} —— 分页单页条数上限（默认不限制）。设为正值时，
 *       超过上限的 pageSize 会被钳制到该值，防止「pageSize=999999」拖垮数据库。</li>
 * </ul>
 *
 * <pre>{@code
 * ywf:
 *   ddd:
 *     mybatisplus:
 *       block-attack-enabled: false     # 默认 true
 *       pagination-max-limit: 500       # 默认不限制
 * }</pre>
 */
@ConfigurationProperties(prefix = "ywf.ddd.mybatisplus")
public record MybatisPlusProperties(
        /** 防全表攻击拦截器开关，默认 {@code true} */
        @DefaultValue("true") boolean blockAttackEnabled,
        /** 分页单页条数上限，默认 {@code null} = 不限制 */
        Long paginationMaxLimit) {
}