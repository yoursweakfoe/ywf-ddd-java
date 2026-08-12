package com.yoursweakfoe.common.pg.handler;

import org.apache.ibatis.type.MappedTypes;

/**
 * Integer 数组类型处理器 —— 用于 MyBatis 与 PostgreSQL INTEGER[] 类型的互转。
 *
 * <p>继承 {@link AbstractArrayTypeHandler}，使用 {@link PgArrayType#INTEGER} 作为 PostgreSQL 类型。
 *
 * @see AbstractArrayTypeHandler
 * @see PgArrayType
 */
@MappedTypes(Integer[].class)
public class IntegerArrayTypeHandler extends AbstractArrayTypeHandler<Integer[]> {

    public IntegerArrayTypeHandler() {
        super(PgArrayType.INTEGER);
    }
}
