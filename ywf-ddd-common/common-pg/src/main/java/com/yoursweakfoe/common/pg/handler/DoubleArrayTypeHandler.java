package com.yoursweakfoe.common.pg.handler;

import org.apache.ibatis.type.MappedTypes;

/**
 * Double 数组类型处理器 —— 用于 MyBatis 与 PostgreSQL FLOAT8[] / DOUBLE PRECISION[] 类型的互转。
 *
 * <p>继承 {@link AbstractArrayTypeHandler}，使用 {@link PgArrayType#FLOAT8} 作为 PostgreSQL 类型。
 *
 * @see AbstractArrayTypeHandler
 * @see PgArrayType
 */
@MappedTypes(Double[].class)
public class DoubleArrayTypeHandler extends AbstractArrayTypeHandler<Double[]> {

    public DoubleArrayTypeHandler() {
        super(PgArrayType.FLOAT8);
    }
}
