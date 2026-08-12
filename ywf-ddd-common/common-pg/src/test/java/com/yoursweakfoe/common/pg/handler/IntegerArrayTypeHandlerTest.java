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
class IntegerArrayTypeHandlerTest {

    private final IntegerArrayTypeHandler handler = new IntegerArrayTypeHandler();

    @Mock private PreparedStatement ps;
    @Mock private ResultSet rs;
    @Mock private Connection conn;
    @Mock private Array sqlArray;

    @Test
    void setNonNullParameter_shouldCreateIntegerArray() throws SQLException {
        Integer[] values = {1, 2, 3};
        when(ps.getConnection()).thenReturn(conn);
        when(conn.createArrayOf("integer", values)).thenReturn(sqlArray);

        handler.setNonNullParameter(ps, 1, values, JdbcType.ARRAY);

        verify(ps).setArray(1, sqlArray);
        verify(sqlArray).free();
    }

    @Test
    void getNullableResult_shouldReturnArray() throws SQLException {
        Integer[] expected = {10, 20, Integer.MAX_VALUE};
        when(rs.getArray("col")).thenReturn(sqlArray);
        when(sqlArray.getArray()).thenReturn(expected);

        Integer[] result = handler.getNullableResult(rs, "col");

        assertThat(result).containsExactly(10, 20, Integer.MAX_VALUE);
        verify(sqlArray).free();
    }

    @Test
    void getNullableResult_shouldReturnNullWhenArrayNull() throws SQLException {
        when(rs.getArray("col")).thenReturn(null);

        assertThat(handler.getNullableResult(rs, "col")).isNull();
    }

    @Test
    void setNonNullParameter_emptyArray() throws SQLException {
        Integer[] values = {};
        when(ps.getConnection()).thenReturn(conn);
        when(conn.createArrayOf("integer", values)).thenReturn(sqlArray);

        handler.setNonNullParameter(ps, 1, values, JdbcType.ARRAY);

        verify(ps).setArray(1, sqlArray);
    }
}
