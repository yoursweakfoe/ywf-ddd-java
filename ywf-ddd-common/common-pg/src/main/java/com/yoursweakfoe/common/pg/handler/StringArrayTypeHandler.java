package com.yoursweakfoe.common.pg.handler;

import com.yoursweakfoe.common.pg.type.PgArrayType;
import org.apache.ibatis.type.MappedTypes;

/**
 * String 数组类型处理器 —— 用于 MyBatis 与 PostgreSQL TEXT[] / VARCHAR[] 类型的互转。
 *
 * <p>继承 {@link AbstractArrayTypeHandler}，使用 {@link PgArrayType#TEXT} 作为 PostgreSQL 类型。
 *
 * @see AbstractArrayTypeHandler
 * @see PgArrayType
 */
@MappedTypes(String[].class)
public class StringArrayTypeHandler extends AbstractArrayTypeHandler<String[]> {

    public StringArrayTypeHandler() {
        super(PgArrayType.TEXT);
    }
}
