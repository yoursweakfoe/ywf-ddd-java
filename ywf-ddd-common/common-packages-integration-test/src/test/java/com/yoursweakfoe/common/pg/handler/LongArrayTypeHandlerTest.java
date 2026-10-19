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
class LongArrayTypeHandlerTest {

    private final LongArrayTypeHandler handler = new LongArrayTypeHandler();

    @Mock private PreparedStatement ps;
    @Mock private ResultSet rs;
    @Mock private Connection conn;
    @Mock private Array sqlArray;

    @Test
    void setNonNullParameter_shouldCreateBigintArray() throws SQLException {
        Long[] values = {100L, Long.MAX_VALUE};
        when(ps.getConnection()).thenReturn(conn);
        when(conn.createArrayOf("bigint", values)).thenReturn(sqlArray);

        handler.setNonNullParameter(ps, 1, values, JdbcType.ARRAY);

        verify(ps).setArray(1, sqlArray);
        verify(sqlArray).free();
    }

    @Test
    void getNullableResult_shouldReturnArray() throws SQLException {
        Long[] expected = {1L, 2L, 3L};
        when(rs.getArray("ids")).thenReturn(sqlArray);
        when(sqlArray.getArray()).thenReturn(expected);

        Long[] result = handler.getNullableResult(rs, "ids");

        assertThat(result).containsExactly(1L, 2L, 3L);
    }

    @Test
    void getNullableResult_shouldReturnNullWhenArrayNull() throws SQLException {
        when(rs.getArray("ids")).thenReturn(null);

        assertThat(handler.getNullableResult(rs, "ids")).isNull();
    }
}
