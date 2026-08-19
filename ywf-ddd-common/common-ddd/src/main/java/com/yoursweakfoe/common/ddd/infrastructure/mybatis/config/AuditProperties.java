package com.yoursweakfoe.common.ddd.infrastructure.mybatis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * DDD 审计字段名配置（record，构造器绑定）。
 *
 * <p>{@code BasicAutoFillHandler} 自动维护创建时间 / 更新时间字段。
 * 字段名默认统一为 {@code createAt} / {@code updateAt}；业务若已存在不同命名的
 * 审计字段（如 {@code created_at} / {@code updated_at}），经本配置覆盖即可，
 * 无需强行改造表结构以对齐框架默认命名。
 *
 * <pre>{@code
 * ywf:
 *   ddd:
 *     audit:
 *       create-field: created_at
 *       update-field: updated_at
 * }</pre>
 */
@ConfigurationProperties(prefix = "ywf.ddd.audit")
public record AuditProperties(
        /** 创建时间字段名，默认 {@code createAt} */
        @DefaultValue("createAt") String createField,
        /** 更新时间字段名，默认 {@code updateAt} */
        @DefaultValue("updateAt") String updateField) {
}