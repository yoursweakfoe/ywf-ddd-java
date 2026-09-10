package com.yoursweakfoe.common.ddd.infrastructure.mybatis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * DDD 审计字段名配置（record，构造器绑定）。
 *
 * <p>{@code AuditFieldFiller} 自动维护创建时间 / 更新时间字段，以及可选的创建人 / 修改人字段。
 * 字段名默认统一为 {@code createdAt} / {@code updatedAt} / {@code createdBy} / {@code updatedBy}
 * （缺省与 BP-S1 PG 原生建表形状逐字同构——列 `created_at` 经 map-underscore 即属性 `createdAt`，
 * 正常消费零配置；历元旧案将缺省从旧缩写 {@code createAt}/{@code updateAt} 随形改定，系破坏性变更）；
 * 业务若已存在不同命名的审计字段（如 {@code creator} / {@code modifier}），
 * 经本配置覆盖即可，无需强行改造表结构以对齐框架默认命名。
 *
 * <h3>操作人字段（createdBy / updatedBy）设计</h3>
 * <ul>
 *   <li><b>字段名是「约定」，不是「开关」</b>：默认 {@code createdBy} / {@code updatedBy}（与时间字段
 *       {@code createdAt} / {@code updatedAt} 命名对称）。填充是否发生，由两道宽松守卫决定——
 *       ① 容器中是否存在 {@link com.yoursweakfoe.common.ddd.infrastructure.mybatis.handler.CurrentUserProvider} Bean；
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
 *       create-field: createdAt         # 默认即 createdAt，历史命名不同才需覆盖
 *       update-field: updatedAt         # 默认即 updatedAt
 *       created-by-field: creator       # 默认 createdBy，历史命名不同才需覆盖
 *       updated-by-field: modifier      # 默认 updatedBy，历史命名不同才需覆盖
 * }</pre>
 */
@ConfigurationProperties(prefix = "ywf.ddd.audit")
public record AuditProperties(
        /** 创建时间字段名，默认 {@code createdAt}（历元旧案随 BP-S1 形状定名） */
        @DefaultValue("createdAt") String createField,
        /** 更新时间字段名，默认 {@code updatedAt} */
        @DefaultValue("updatedAt") String updateField,
        /** 创建人字段名，默认 {@code createdBy}（可配空串关闭操作人填充） */
        @DefaultValue("createdBy") String createdByField,
        /** 修改人字段名，默认 {@code updatedBy}（可配空串关闭操作人填充） */
        @DefaultValue("updatedBy") String updatedByField) {
}
