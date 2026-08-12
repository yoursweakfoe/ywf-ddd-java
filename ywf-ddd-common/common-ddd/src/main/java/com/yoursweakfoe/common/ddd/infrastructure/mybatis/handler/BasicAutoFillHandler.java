package com.yoursweakfoe.common.ddd.infrastructure.mybatis.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import java.time.OffsetDateTime;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

/**
 * MyBatis-Plus 自动填充处理器 —— 自动维护创建时间和更新时间字段。
 *
 * <p>本处理器在插入和更新操作时自动填充 createAt 和 updateAt 字段，
 * 避免在业务代码中手动设置时间字段。
 *
 * <p>时间类型统一使用 {@link OffsetDateTime}（带时区偏移，跨地域无歧义）。
 * PO 中的 createAt / updateAt 字段应声明为 {@code OffsetDateTime} 类型。
 *
 * @see com.baomidou.mybatisplus.core.handlers.MetaObjectHandler
 */
@Component
public class BasicAutoFillHandler implements MetaObjectHandler {

    /** 插入时自动填充 createAt + updateAt */
    @Override
    public void insertFill(MetaObject metaObject) {
        OffsetDateTime now = OffsetDateTime.now();
        this.strictInsertFill(metaObject, "createAt", OffsetDateTime.class, now);
        this.strictInsertFill(metaObject, "updateAt", OffsetDateTime.class, now);
    }

    /**
     * 更新时自动填充 updateAt。
     *
     * <p>使用 {@code setFieldValByName} 而非 {@code strictUpdateFill}，
     * 以确保每次 UPDATE 均刷新 updateAt（无论 PO 是否标注 {@code @TableField(fill = FieldFill.UPDATE)}）。
     * 若 PO 不含 updateAt 字段，该调用静默忽略，不会报错。
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        OffsetDateTime now = OffsetDateTime.now();
        setFieldValByName("updateAt", now, metaObject);
    }
}
