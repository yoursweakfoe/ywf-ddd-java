package com.yoursweakfoe.common.pg.handler;

import com.yoursweakfoe.common.pg.type.PgArrayType;
import java.util.UUID;
import org.apache.ibatis.type.MappedTypes;

/**
 * UUID 数组类型处理器 —— 用于 MyBatis 与 PostgreSQL UUID[] 类型的互转。
 *
 * <p>继承 {@link AbstractArrayTypeHandler}，使用 {@link PgArrayType#UUID} 作为 PostgreSQL 类型。
 *
 * @see AbstractArrayTypeHandler
 * @see PgArrayType
 */
@MappedTypes(UUID[].class)
public class UUIDArrayTypeHandler extends AbstractArrayTypeHandler<UUID[]> {

    public UUIDArrayTypeHandler() {
        super(PgArrayType.UUID);
    }
}
