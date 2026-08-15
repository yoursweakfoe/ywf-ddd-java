package com.yoursweakfoe.common.ddd.infrastructure.mybatis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * DDD 审计字段名配置。
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
public class AuditProperties {

    /** 创建时间字段名，默认 {@code createAt} */
    private String createField = "createAt";

    /** 更新时间字段名，默认 {@code updateAt} */
    private String updateField = "updateAt";

    public String getCreateField() {
        return createField;
    }

    public void setCreateField(String createField) {
        this.createField = createField;
    }

    public String getUpdateField() {
        return updateField;
    }

    public void setUpdateField(String updateField) {
        this.updateField = updateField;
    }
}
