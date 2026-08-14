package com.yoursweakfoe.common.pg.type;

import lombok.Getter;

/**
 * PostgreSQL 数组类型枚举 —— 定义支持的数组元素类型。
 *
 * <p>枚举值与 PostgreSQL 类型名称一一对应，用于 {@link com.yoursweakfoe.common.pg.handler.AbstractArrayTypeHandler} 创建 SQL
 * Array 时指定类型。
 *
 * <p><strong>设计说明：</strong> 使用枚举而非字符串常量，是为了在编译期严格限制可用的 PostgreSQL 数组类型， 避免拼写错误，并提供类型安全。
 *
 * <p><strong>与 TypeHandler 自动发现的关系：</strong> 每个枚举值对应一个具体的数组处理器（如 {@code TEXT} 对应 {@link
 * com.yoursweakfoe.common.pg.handler.StringArrayTypeHandler}）。 这些处理器通过 {@code @MappedTypes(Xxx[].class)} 注解向 MyBatis
 * 注册， 使得 PO 实体类中的数组字段可以自动匹配到正确的处理器，无需显式指定。
 *
 * @see com.yoursweakfoe.common.pg.handler.AbstractArrayTypeHandler
 * @see org.apache.ibatis.type.MappedTypes
 */
@Getter
public enum PgArrayType {

    /** TEXT 类型 —— 对应 Java String[] */
    TEXT("text"),

    /** INT2 / SMALLINT 类型 —— 对应 Java Short[] */
    INT2("int2"),

    /** INTEGER / INT4 类型 —— 对应 Java Integer[] */
    INTEGER("integer"),

    /** BIGINT / INT8 类型 —— 对应 Java Long[] */
    BIGINT("bigint"),

    /** REAL / FLOAT4 类型 —— 对应 Java Float[] */
    FLOAT4("float4"),

    /** DOUBLE PRECISION / FLOAT8 类型 —— 对应 Java Double[] */
    FLOAT8("float8"),

    /** BOOLEAN 类型 —— 对应 Java Boolean[] */
    BOOLEAN("boolean"),

    /** UUID 类型 —— 对应 Java UUID[] */
    UUID("uuid");

    private final String pgTypeName;

    PgArrayType(String pgTypeName) {
        this.pgTypeName = pgTypeName;
    }
}
