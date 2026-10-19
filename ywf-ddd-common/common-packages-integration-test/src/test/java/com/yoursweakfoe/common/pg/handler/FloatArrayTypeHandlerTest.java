package com.yoursweakfoe.common.pg.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FloatArrayTypeHandlerTest {

    private final FloatArrayTypeHandler handler = new FloatArrayTypeHandler();

    @Mock private PreparedStatement ps;
    @Mock private ResultSet rs;
    @Mock private Connection conn;
    @Mock private Array sqlArray;

    @Test
    void setNonNullParameter_shouldCreateFloat4Array() throws SQLException {
        Float[] values = {1.0f, 2.5f};
        when(ps.getConnection()).thenReturn(conn);
        when(conn.createArrayOf("float4", values)).thenReturn(sqlArray);

        handler.setNonNullParameter(ps, 1, values, JdbcType.ARRAY);

        verify(ps).setArray(1, sqlArray);
        verify(sqlArray).free();
    }

    @Test
    void getNullableResult_shouldReturnArray() throws SQLException {
        Float[] expected = {0.1f, 0.2f};
        when(rs.getArray("vals")).thenReturn(sqlArray);
        when(sqlArray.getArray()).thenReturn(expected);

        Float[] result = handler.getNullableResult(rs, "vals");

        assertThat(result).containsExactly(0.1f, 0.2f);
    }

    @Test
    void getNullableResult_shouldReturnNullWhenArrayNull() throws SQLException {
        when(rs.getArray("vals")).thenReturn(null);

        assertThat(handler.getNullableResult(rs, "vals")).isNull();
    }
}
