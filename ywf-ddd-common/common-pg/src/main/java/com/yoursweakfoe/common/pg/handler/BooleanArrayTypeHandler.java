package com.yoursweakfoe.common.pg.handler;

import org.apache.ibatis.type.MappedTypes;

/**
 * Boolean 数组类型处理器 —— 用于 MyBatis 与 PostgreSQL BOOLEAN[] 类型的互转。
 *
 * <p>继承 {@link AbstractArrayTypeHandler}，使用 {@link PgArrayType#BOOLEAN} 作为 PostgreSQL 类型。
 *
 * @see AbstractArrayTypeHandler
 * @see PgArrayType
 */
@MappedTypes(Boolean[].class)
public class BooleanArrayTypeHandler extends AbstractArrayTypeHandler<Boolean[]> {

    public BooleanArrayTypeHandler() {
        super(PgArrayType.BOOLEAN);
    }
}
