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
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * JsonNode 类型处理器 —— 用于 MyBatis 与 PostgreSQL JSONB 类型的互转（Jackson JsonNode 映射）。
 *
 * <p>本处理器将 PostgreSQL JSONB 列与 Jackson 的 JsonNode 进行双向转换， 适用于需要在 Java 层对 JSON 进行结构化读写的场景。
 *
 * <p>写入时将 JsonNode 序列化为 JSON 字符串并通过 PGobject 传入， 读取时将字符串反序列化为 JsonNode 对象。
 *
 * <p>使用本处理器需要确保 classpath 中包含 Jackson Databind 依赖。
 *
 * <p><strong>使用说明：</strong> 由于 {@code JsonNode} 是特定类型，本处理器通过 {@code @MappedTypes(JsonNode.class)}
 * 自动注册。在 PO 实体类中使用时，<strong>推荐</strong>显式指定以确保清晰：
 *
 * <pre>
 * {@code @TableField(value = "config", typeHandler = JsonNodeTypeHandler.class)}
 * private JsonNode config;
 * </pre>
 *
 * @see tools.jackson.databind.JsonNode
 * @see org.apache.ibatis.type.BaseTypeHandler
 * @see JsonbTypeHandler
 */
@MappedTypes(JsonNode.class)
@MappedJdbcTypes(JdbcType.OTHER)
public class JsonNodeTypeHandler extends BaseTypeHandler<JsonNode> {

    private static final JsonMapper OBJECT_MAPPER = new JsonMapper();

    /** 序列化 JsonNode 并通过 PGobject 写入 jsonb */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, JsonNode param, JdbcType jdbcType)
            throws SQLException {
        PGobject pgObject = new PGobject();
        pgObject.setType("jsonb");
        pgObject.setValue(param.toString());
        ps.setObject(i, pgObject);
    }

    /** {@inheritDoc} */
    @Override
    public JsonNode getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseJson(rs.getString(columnName));
    }

    /** {@inheritDoc} */
    @Override
    public JsonNode getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseJson(rs.getString(columnIndex));
    }

    /** {@inheritDoc} */
    @Override
    public JsonNode getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseJson(cs.getString(columnIndex));
    }

    /** 解析 JSON 字符串为 JsonNode，null 安全 */
    private JsonNode parseJson(String json) throws SQLException {
        if (json == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (JacksonException e) {
            throw new SQLException("Failed to parse JSON string to JsonNode", e);
        }
    }
}
