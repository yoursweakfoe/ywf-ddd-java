package com.yoursweakfoe.common.ddd.infrastructure.mybatisplus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * DDD 审计字段名配置（record，构造器绑定）。
 *
 * <p>{@code BasicAutoFillHandler} 自动维护创建时间 / 更新时间字段，以及可选的创建人 / 修改人字段。
 * 字段名默认统一为 {@code createAt} / {@code updateAt} / {@code createdBy} / {@code updatedBy}；
 * 业务若已存在不同命名的审计字段（如 {@code created_at} / {@code updated_at} / {@code creator}），
 * 经本配置覆盖即可，无需强行改造表结构以对齐框架默认命名。
 *
 * <h3>操作人字段（createdBy / updatedBy）设计</h3>
 * <ul>
 *   <li><b>字段名是「约定」，不是「开关」</b>：默认 {@code createdBy} / {@code updatedBy}（与时间字段
 *       {@code createAt} / {@code updateAt} 命名对称）。填充是否发生，由两道宽松守卫决定——
 *       ① 容器中是否存在 {@link com.yoursweakfoe.common.ddd.infrastructure.mybatisplus.handler.CurrentUserProvider} Bean；
 *       ② PO 是否声明了该字段（{@code hasSetter}）。二者任一不满足即静默跳过。</li>
 *   <li><b>高度宽松可选</b>：即使表里建了操作人列，业务未提供 {@code CurrentUserProvider} 时依旧不填
 *       （不报错、不写 null 覆盖已有值）；字段名可显式配为空串以彻底关闭操作人填充。</li>
 *   <li><b>类型宽松</b>：操作人字段的 Java 类型不做死（{@code String} 账号 / {@code Long} 工号均可），
 *       由 {@code CurrentUserProvider} 的返回类型与 PO 字段声明类型对齐即可，框架不强校验。</li>
 * </ul>
 *
 * <pre>{@code
 * ywf:
 *   ddd:
 *     audit:
 *       create-field: create_at
 *       update-field: update_at
 *       created-by-field: creator       # 默认 createdBy，历史命名不同才需覆盖
 *       updated-by-field: modifier      # 默认 updatedBy，历史命名不同才需覆盖
 * }</pre>
 */
@ConfigurationProperties(prefix = "ywf.ddd.audit")
public record AuditProperties(
        /** 创建时间字段名，默认 {@code createAt} */
        @DefaultValue("createAt") String createField,
        /** 更新时间字段名，默认 {@code updateAt} */
        @DefaultValue("updateAt") String updateField,
        /** 创建人字段名，默认 {@code createdBy}（可配空串关闭操作人填充） */
        @DefaultValue("createdBy") String createdByField,
        /** 修改人字段名，默认 {@code updatedBy}（可配空串关闭操作人填充） */
        @DefaultValue("updatedBy") String updatedByField) {
}