package com.yoursweakfoe.common.ddd.infrastructure.mybatis.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.yoursweakfoe.common.ddd.infrastructure.mybatis.config.AuditProperties;
import java.time.OffsetDateTime;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.beans.factory.ObjectProvider;

/**
 * MyBatis-Plus 自动填充处理器 —— 自动维护创建时间 / 更新时间字段，以及可选的创建人 / 修改人字段。
 *
 * <p>本处理器在插入和更新操作时自动填充审计字段，避免在业务代码中手动设置。
 * 时间字段（createAt / updateAt）为 {@code OffsetDateTime}；操作人字段（createdBy / updatedBy）
 * 高度宽松可选——只有配置了字段名 + 容器中存在 {@link CurrentUserProvider} Bean 时才填充。
 *
 * <p><strong>填充触发前提（重要）</strong>：MyBatis-Plus 的 {@code MybatisParameterHandler}
 * 仅在 {@code TableInfo.isWithInsertFill()} / {@code isWithUpdateFill()} 为 {@code true} 时才调用
 * 本处理器的 {@code insertFill} / {@code updateFill}——而该 flag 要求 PO 的字段标注
 * {@code @TableField(fill = FieldFill.INSERT)} / {@code @TableField(fill = FieldFill.UPDATE)}
 * （或 {@code INSERT_UPDATE}）。故业务 PO 的 {@code createAt} / {@code updateAt} / {@code createdBy} /
 * {@code updatedBy} 字段<strong>必须</strong>标注相应的 {@code @TableField(fill = ...)} 注解，
 * 否则这些列不会进入生成的 SQL，填充不会生效。
 *
 * @see com.baomidou.mybatisplus.core.handlers.MetaObjectHandler
 * @see CurrentUserProvider
 */
public class BasicAutoFillHandler implements MetaObjectHandler {

    private final AuditProperties auditProperties;
    private final ObjectProvider<CurrentUserProvider> currentUserProvider;

    public BasicAutoFillHandler(AuditProperties auditProperties,
                                ObjectProvider<CurrentUserProvider> currentUserProvider) {
        this.auditProperties = auditProperties;
        this.currentUserProvider = currentUserProvider;
    }

    /** 插入时填充创建/更新时间 + 可选操作人（字段名经 ywf.ddd.audit.* 可配置） */
    @Override
    public void insertFill(MetaObject metaObject) {
        OffsetDateTime now = OffsetDateTime.now();
        strictInsertFill(metaObject, auditProperties.createField(), OffsetDateTime.class, now);
        strictInsertFill(metaObject, auditProperties.updateField(), OffsetDateTime.class, now);
        fillUser(metaObject, auditProperties.createdByField());
        fillUser(metaObject, auditProperties.updatedByField());
    }

    /**
     * 更新时无条件刷新 updateAt + 可选修改人。
     *
     * <p>使用 {@code setFieldValByName}（无条件）而非 {@code strictUpdateFill}：
     * updateAt 应始终刷新为最新（逻辑删除也经此路径，见 {@code DeleteById} 的 UPDATE fill 段）。
     * 逻辑删除不设独立 deletedAt/deletedBy——表审计只表达「最后状态」，删除发生在 updateAt 上复用。
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        OffsetDateTime now = OffsetDateTime.now();
        setFieldValByName(auditProperties.updateField(), now, metaObject);
        fillUser(metaObject, auditProperties.updatedByField());
    }

    /**
     * 填充操作人字段（类型宽松、高度可选）。
     *
     * <p>字段名默认为 {@code createdBy} / {@code updatedBy}（约定）；填不填由两道宽松守卫决定：
     * ① 容器中无 {@link CurrentUserProvider} Bean → 跳过；② provider 返回 {@code null} → 跳过；
     * ③ PO 未声明该字段（{@code hasSetter}=false）→ 跳过；④ 字段已有值（业务显式指定）→ 不覆盖。
     * 字段名显式配为空串时彻底关闭操作人填充。
     */
    private void fillUser(MetaObject metaObject, String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
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
        if (metaObject.hasSetter(fieldName) && getFieldValByName(fieldName, metaObject) == null) {
            setFieldValByName(fieldName, user, metaObject);
        }
    }
}