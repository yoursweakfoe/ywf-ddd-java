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
class DoubleArrayTypeHandlerTest {

    private final DoubleArrayTypeHandler handler = new DoubleArrayTypeHandler();

    @Mock private PreparedStatement ps;
    @Mock private ResultSet rs;
    @Mock private Connection conn;
    @Mock private Array sqlArray;

    @Test
    void setNonNullParameter_shouldCreateFloat8Array() throws SQLException {
        Double[] values = {1.1, 2.2, Double.MAX_VALUE};
        when(ps.getConnection()).thenReturn(conn);
        when(conn.createArrayOf("float8", values)).thenReturn(sqlArray);

        handler.setNonNullParameter(ps, 1, values, JdbcType.ARRAY);

        verify(ps).setArray(1, sqlArray);
        verify(sqlArray).free();
    }

    @Test
    void getNullableResult_shouldReturnArray() throws SQLException {
        Double[] expected = {3.14, 2.71};
        when(rs.getArray("scores")).thenReturn(sqlArray);
        when(sqlArray.getArray()).thenReturn(expected);

        Double[] result = handler.getNullableResult(rs, "scores");

        assertThat(result).containsExactly(3.14, 2.71);
    }

    @Test
    void getNullableResult_shouldReturnNullWhenArrayNull() throws SQLException {
        when(rs.getArray("scores")).thenReturn(null);

        assertThat(handler.getNullableResult(rs, "scores")).isNull();
    }
}
