package com.yoursweakfoe.common.pg.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

/**
 * UUID 类型处理器 —— 用于 MyBatis 与 PostgreSQL UUID 类型的互转。
 *
 * <p>由于 PostgreSQL 的 UUID 类型对应 JDBC 的 JdbcType.OTHER， 需要自定义 TypeHandler 才能正确处理。本处理器将 Java 的 UUID 对象
 * 与数据库的 UUID 类型进行自动转换。
 *
 * <p><strong>使用说明：</strong> 本处理器通过 {@code @MappedTypes(UUID.class)} 注解自动注册为 UUID 类型的全局处理器。 因此，在 PO
 * 实体类中使用 {@code private UUID id;} 时，手写 XML 语句中<strong>无需</strong>显式指定
 * {@code typeHandler}，MyBatis 会自动匹配。
 *
 * <p>这种自动发现机制适用于 Java 类型与 TypeHandler 的映射是<strong>唯一</strong>的场景。 如果同一 Java 类型有多个 TypeHandler（如
 * String 可以是普通字符串或 JSONB）， 则需要在 XML 语句中显式指定：参数位 {@code #{prop, typeHandler=全限定类名}}、
 * 结果位 {@code <result ... typeHandler="全限定类名"/>}。
 *
 * @see org.apache.ibatis.type.BaseTypeHandler
 * @see org.apache.ibatis.type.MappedTypes
 */
@MappedTypes(UUID.class)
@MappedJdbcTypes(JdbcType.OTHER) // PostgreSQL UUID 对应 JdbcType.OTHER
public class UUIDTypeHandler extends BaseTypeHandler<UUID> {

    /** 直接传递 UUID 对象，PG JDBC 驱动自动处理 */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, UUID param, JdbcType jdbcType)
            throws SQLException {
        ps.setObject(i, param);
    }

    /** {@inheritDoc} */
    @Override
    public UUID getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return (UUID) rs.getObject(columnName);
    }

    /** {@inheritDoc} */
    @Override
    public UUID getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return (UUID) rs.getObject(columnIndex);
    }

    /** {@inheritDoc} */
    @Override
    public UUID getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return (UUID) cs.getObject(columnIndex);
    }
}
