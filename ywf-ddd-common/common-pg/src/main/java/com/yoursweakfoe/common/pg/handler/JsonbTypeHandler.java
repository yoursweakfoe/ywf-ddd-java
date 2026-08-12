package com.yoursweakfoe.common.pg.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

/**
 * JSONB 类型处理器 —— 用于 MyBatis 与 PostgreSQL JSONB 类型的互转（String 映射）。
 *
 * <p>PostgreSQL 的 JSONB 类型在 JDBC 层面对应 JdbcType.OTHER， 写入时需要使用 PGobject 显式指定类型为 "jsonb"，
 * 读取时则可以直接获取为字符串。
 *
 * <p>本处理器将 Java String 与数据库 JSONB 类型进行自动转换， 适用于不需要在 Java 层做 JSON 结构化处理的场景。
 *
 * <p><strong>使用说明：</strong> 由于 {@code String} 类型有多个 TypeHandler（默认的 StringTypeHandler 和本处理器）， 在 PO
 * 实体类中使用 JSONB 字段时，<strong>必须</strong>显式指定本处理器：
 *
 * <pre>
 * {@code @TableField(value = "metadata", typeHandler = JsonbTypeHandler.class)}
 * private String metadata;
 * </pre>
 *
 * 否则 MyBatis 会默认使用 StringTypeHandler，导致 PostgreSQL 类型不匹配错误。
 *
 * @see org.apache.ibatis.type.BaseTypeHandler
 * @see JsonNodeTypeHandler
 */
@MappedTypes(String.class)
@MappedJdbcTypes(JdbcType.OTHER)
public class JsonbTypeHandler extends BaseTypeHandler<String> {

    /** 通过 PGobject 设置类型为 "jsonb"，否则 PG 会识别为 VARCHAR */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String param, JdbcType jdbcType)
            throws SQLException {
        PGobject pgObject = new PGobject();
        pgObject.setType("jsonb");
        pgObject.setValue(param);
        ps.setObject(i, pgObject);
    }

    /** {@inheritDoc} */
    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getString(columnName);
    }

    /** {@inheritDoc} */
    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getString(columnIndex);
    }

    /** {@inheritDoc} */
    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return cs.getString(columnIndex);
    }
}
