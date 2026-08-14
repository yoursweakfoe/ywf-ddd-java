package com.yoursweakfoe.common.pg.handler;

import com.yoursweakfoe.common.pg.type.PgArrayType;
import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;

/**
 * PostgreSQL 数组类型处理器抽象基类 —— 提供通用的数组类型转换逻辑。
 *
 * <p>子类只需指定对应的 {@link PgArrayType} 枚举值和 Java 数组类型， 即可自动完成 PostgreSQL 数组与 Java 数组的双向转换。
 *
 * <p><strong>使用方式：</strong>
 *
 * <pre>
 * public class StringArrayTypeHandler extends AbstractArrayTypeHandler&lt;String[]&gt; {
 *     public StringArrayTypeHandler() {
 *         super(PgArrayType.TEXT);
 *     }
 * }
 * </pre>
 *
 * <p><strong>TypeHandler 自动发现机制说明：</strong>
 *
 * <ul>
 *   <li>每个具体数组处理器通过 {@code @MappedTypes(Xxx[].class)} 注解注册
 *   <li>由于各数组类型的 Java Class 不同（String[].class、Integer[].class 等）， 映射是<strong>唯一</strong>的，MyBatis
 *       可以自动匹配
 *   <li>因此，在 PO 实体类中使用数组字段时，<strong>通常无需</strong>显式指定 typeHandler
 * </ul>
 *
 * 例如：
 *
 * <pre>
 * private String[] tags;      // 自动使用 StringArrayTypeHandler
 * private Integer[] scores;   // 自动使用 IntegerArrayTypeHandler
 * private UUID[] relatedIds;  // 自动使用 UUIDArrayTypeHandler
 * </pre>
 *
 * <p><strong>何时需要显式指定：</strong> 如果同一 Java 数组类型需要映射到不同的 PostgreSQL 类型（极少见）， 可以在字段上使用
 * {@code @TableField(typeHandler = ...)} 显式指定。
 *
 * @param <T> Java 数组类型，如 String[]、Integer[] 等
 * @see PgArrayType
 * @see org.apache.ibatis.type.BaseTypeHandler
 * @see org.apache.ibatis.type.MappedTypes
 */
@MappedJdbcTypes(JdbcType.ARRAY)
public abstract class AbstractArrayTypeHandler<T> extends BaseTypeHandler<T> {

    private final PgArrayType pgArrayType;

    /**
     * 构造数组类型处理器。
     *
     * @param pgArrayType PostgreSQL 数组元素类型枚举
     */
    protected AbstractArrayTypeHandler(PgArrayType pgArrayType) {
        this.pgArrayType = pgArrayType;
    }

    /**
     * 将 Java 数组参数转换为 PostgreSQL 数组并设置到 PreparedStatement 中。
     *
     * @param ps PreparedStatement
     * @param i 参数索引（从 1 开始）
     * @param param Java 数组参数值
     * @param jdbcType JDBC 类型
     * @throws SQLException 如果设置参数时发生数据库错误
     */
    @Override
    @SuppressWarnings("unchecked")
    public void setNonNullParameter(PreparedStatement ps, int i, T param, JdbcType jdbcType)
            throws SQLException {
        Connection conn = ps.getConnection();
        // 将数组转换为 Object[] 以适配 createArrayOf
        Object[] arrayData = (Object[]) param;
        Array array = conn.createArrayOf(pgArrayType.getPgTypeName(), arrayData);
        try {
            ps.setArray(i, array);
        } finally {
            array.free();
        }
    }

    /** {@inheritDoc} */
    @Override
    public T getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return extractArray(rs.getArray(columnName));
    }

    /** {@inheritDoc} */
    @Override
    public T getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return extractArray(rs.getArray(columnIndex));
    }

    /** {@inheritDoc} */
    @Override
    public T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return extractArray(cs.getArray(columnIndex));
    }

    /** 将 java.sql.Array 转为 Java 数组，null 安全 */
    @SuppressWarnings("unchecked")
    private T extractArray(Array sqlArray) throws SQLException {
        if (sqlArray == null) {
            return null;
        }
        try {
            return (T) sqlArray.getArray();
        } finally {
            sqlArray.free();
        }
    }
}
