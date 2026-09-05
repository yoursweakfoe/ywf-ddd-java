package com.yoursweakfoe.common.ddd.infrastructure.mybatis.handler;

import com.yoursweakfoe.common.ddd.infrastructure.mybatis.config.AuditProperties;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 审计字段填充器 —— 自动维护创建时间 / 更新时间字段，以及可选的创建人 / 修改人字段。
 *
 * <p>本组件在插入和更新前填充审计字段，避免业务代码手动设置。基于 MyBatis 核心反射 API
 * {@link MetaObject}（按字段名读写，无任何注解依赖——PO 的审计字段无需标注任何标记）。
 * 填充由 {@code MybatisPersistence} 在 {@code mapper.insert} / {@code mapper.updateById}
 * 前<strong>显式调用</strong>——触发链透明，无拦截器魔法，可在数据链路上直接 grep。
 *
 * <p>时间字段（createAt / updateAt）为 {@code OffsetDateTime}；操作人字段（createdBy / updatedBy）
 * 高度宽松可选——只有配置了字段名 + 容器中存在 {@link CurrentUserProvider} Bean 时才填充。
 *
 * <p><strong>时间源注入</strong>：时间取自构造器注入的 {@link Clock}
 * （<strong>纯 JDK 类型，非 Spring 类</strong>——不引入任何新依赖）。自动配置提供
 * {@code @ConditionalOnMissingBean} 的 {@code Clock}（{@code systemUTC} —— 存储/运算一律 UTC，
 * 与部署环境时区解耦，见 {@code ClockAutoConfiguration}）；业务测试可自定义固定 Clock Bean 覆盖，
 * 使审计时间断言确定化。
 *
 * <p><strong>逻辑删除的审计刷新</strong>不经过本组件——删除语句无 PO 可填，
 * 由 {@code MybatisPersistence} 将 {@code now} / {@code updatedBy} 作为 SQL 参数传给
 * {@code deleteById} / {@code deleteByIds}，在 XML 的 SET 子句内刷新。
 */
public class AuditFieldFiller {

    private final AuditProperties auditProperties;
    private final ObjectProvider<CurrentUserProvider> currentUserProvider;
    private final Clock clock;

    public AuditFieldFiller(AuditProperties auditProperties,
                            ObjectProvider<CurrentUserProvider> currentUserProvider,
                            Clock clock) {
        this.auditProperties = auditProperties;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    /** 插入时填充创建/更新时间 + 可选操作人（字段名经 ywf.ddd.audit.* 可配置；已有值不覆盖） */
    public void fillInsert(Object po) {
        MetaObject metaObject = SystemMetaObject.forObject(po);
        OffsetDateTime now = OffsetDateTime.now(clock);
        strictFill(metaObject, auditProperties.createField(), now);
        strictFill(metaObject, auditProperties.updateField(), now);
        fillUser(metaObject, auditProperties.createdByField());
        fillUser(metaObject, auditProperties.updatedByField());
    }

    /**
     * 更新时无条件刷新 updateAt + 可选修改人。
     *
     * <p>updateAt <strong>无条件覆盖</strong>（区别于 {@link #fillInsert} 的「有值不覆盖」）：
     * updateAt 应始终刷新为最新。逻辑删除不设独立 deletedAt/deletedBy——表审计只表达
     * 「最后状态」，删除发生在 updateAt 上复用（经 delete 语句的 {@code now} 参数刷新）。
     */
    public void fillUpdate(Object po) {
        MetaObject metaObject = SystemMetaObject.forObject(po);
        OffsetDateTime now = OffsetDateTime.now(clock);
        String updateField = auditProperties.updateField();
        if (isEnabled(updateField) && metaObject.hasSetter(updateField)) {
            metaObject.setValue(updateField, now);
        }
        fillUser(metaObject, auditProperties.updatedByField());
    }

    /** 时间字段宽松填充：字段名已配置 + PO 声明该字段 + 当前值为 null 才写入（业务显式指定不覆盖） */
    private void strictFill(MetaObject metaObject, String fieldName, Object value) {
        if (isEnabled(fieldName)
                && metaObject.hasSetter(fieldName)
                && metaObject.getValue(fieldName) == null) {
            metaObject.setValue(fieldName, value);
        }
    }

    /**
     * 填充操作人字段（类型宽松、高度可选）。
     *
     * <p>字段名默认为 {@code createdBy} / {@code updatedBy}（约定）；填不填由四道宽松守卫决定：
     * ① 字段名配置为空 → 关闭；② 容器中无 {@link CurrentUserProvider} Bean → 跳过；
     * ③ provider 返回 {@code null} → 跳过；④ PO 未声明该字段（{@code hasSetter}=false）→ 跳过；
     * 字段已有值（业务显式指定）→ 不覆盖。
     */
    private void fillUser(MetaObject metaObject, String fieldName) {
        if (!isEnabled(fieldName)) {
            return;
        }
        CurrentUserProvider provider = currentUserProvider.getIfAvailable();
        if (provider == null) {
            return;
        }
        Object user = provider.currentUser();
        if (user == null) {
            return;
        }
        // 有值不覆盖（业务显式指定操作人时尊重之），字段缺失时静默忽略（hasSetter 守卫）
        if (metaObject.hasSetter(fieldName) && metaObject.getValue(fieldName) == null) {
            metaObject.setValue(fieldName, user);
        }
    }

    /** 字段名显式配为空串 = 该审计字段填充彻底关闭 */
    private static boolean isEnabled(String fieldName) {
        return fieldName != null && !fieldName.isBlank();
    }
}
