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
class BooleanArrayTypeHandlerTest {

    private final BooleanArrayTypeHandler handler = new BooleanArrayTypeHandler();

    @Mock private PreparedStatement ps;
    @Mock private ResultSet rs;
    @Mock private Connection conn;
    @Mock private Array sqlArray;

    @Test
    void setNonNullParameter_shouldCreateBooleanArray() throws SQLException {
        Boolean[] values = {true, false, true};
        when(ps.getConnection()).thenReturn(conn);
        when(conn.createArrayOf("boolean", values)).thenReturn(sqlArray);

        handler.setNonNullParameter(ps, 1, values, JdbcType.ARRAY);

        verify(ps).setArray(1, sqlArray);
        verify(sqlArray).free();
    }

    @Test
    void getNullableResult_shouldReturnArray() throws SQLException {
        Boolean[] expected = {true, false};
        when(rs.getArray("flags")).thenReturn(sqlArray);
        when(sqlArray.getArray()).thenReturn(expected);

        Boolean[] result = handler.getNullableResult(rs, "flags");

        assertThat(result).containsExactly(true, false);
    }

    @Test
    void getNullableResult_shouldReturnNullWhenArrayNull() throws SQLException {
        when(rs.getArray("flags")).thenReturn(null);

        assertThat(handler.getNullableResult(rs, "flags")).isNull();
    }
}
