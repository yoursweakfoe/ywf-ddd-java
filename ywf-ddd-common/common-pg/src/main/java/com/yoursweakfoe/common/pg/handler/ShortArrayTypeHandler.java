package com.yoursweakfoe.common.pg.handler;

import org.apache.ibatis.type.MappedTypes;

/**
 * Short 数组类型处理器 —— 用于 MyBatis 与 PostgreSQL INT2[] / SMALLINT[] 类型的互转。
 *
 * <p>继承 {@link AbstractArrayTypeHandler}，使用 {@link PgArrayType#INT2} 作为 PostgreSQL 类型。
 *
 * @see AbstractArrayTypeHandler
 * @see PgArrayType
 */
@MappedTypes(Short[].class)
public class ShortArrayTypeHandler extends AbstractArrayTypeHandler<Short[]> {

    public ShortArrayTypeHandler() {
        super(PgArrayType.INT2);
    }
}
