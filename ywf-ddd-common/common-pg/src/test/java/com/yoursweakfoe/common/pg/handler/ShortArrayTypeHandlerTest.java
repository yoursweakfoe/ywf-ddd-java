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
class ShortArrayTypeHandlerTest {

    private final ShortArrayTypeHandler handler = new ShortArrayTypeHandler();

    @Mock private PreparedStatement ps;
    @Mock private ResultSet rs;
    @Mock private Connection conn;
    @Mock private Array sqlArray;

    @Test
    void setNonNullParameter_shouldCreateInt2Array() throws SQLException {
        Short[] values = {(short) 1, (short) 2, Short.MAX_VALUE};
        when(ps.getConnection()).thenReturn(conn);
        when(conn.createArrayOf("int2", values)).thenReturn(sqlArray);

        handler.setNonNullParameter(ps, 1, values, JdbcType.ARRAY);

        verify(ps).setArray(1, sqlArray);
        verify(sqlArray).free();
    }

    @Test
    void getNullableResult_shouldReturnArray() throws SQLException {
        Short[] expected = {(short) 10, (short) 20};
        when(rs.getArray("codes")).thenReturn(sqlArray);
        when(sqlArray.getArray()).thenReturn(expected);

        Short[] result = handler.getNullableResult(rs, "codes");

        assertThat(result).containsExactly((short) 10, (short) 20);
    }

    @Test
    void getNullableResult_shouldReturnNullWhenArrayNull() throws SQLException {
        when(rs.getArray("codes")).thenReturn(null);

        assertThat(handler.getNullableResult(rs, "codes")).isNull();
    }
}
