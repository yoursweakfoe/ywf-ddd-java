package com.yoursweakfoe.common.pg.handler;

import com.yoursweakfoe.common.pg.type.PgArrayType;
import org.apache.ibatis.type.MappedTypes;

/**
 * Long 数组类型处理器 —— 用于 MyBatis 与 PostgreSQL BIGINT[] 类型的互转。
 *
 * <p>继承 {@link AbstractArrayTypeHandler}，使用 {@link PgArrayType#BIGINT} 作为 PostgreSQL 类型。
 *
 * @see AbstractArrayTypeHandler
 * @see PgArrayType
 */
@MappedTypes(Long[].class)
public class LongArrayTypeHandler extends AbstractArrayTypeHandler<Long[]> {

    public LongArrayTypeHandler() {
        super(PgArrayType.BIGINT);
    }
}
