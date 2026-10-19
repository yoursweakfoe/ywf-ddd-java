package com.yoursweakfoe.common.pg.handler;

import com.yoursweakfoe.common.pg.type.PgArrayType;
import org.apache.ibatis.type.MappedTypes;

/**
 * Float 数组类型处理器 —— 用于 MyBatis 与 PostgreSQL FLOAT4[] / REAL[] 类型的互转。
 *
 * <p>继承 {@link AbstractArrayTypeHandler}，使用 {@link PgArrayType#FLOAT4} 作为 PostgreSQL 类型。
 *
 * @see AbstractArrayTypeHandler
 * @see PgArrayType
 */
@MappedTypes(Float[].class)
public class FloatArrayTypeHandler extends AbstractArrayTypeHandler<Float[]> {

    public FloatArrayTypeHandler() {
        super(PgArrayType.FLOAT4);
    }
}
