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

/**
 * StringArrayTypeHandler 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class StringArrayTypeHandlerTest {

    private final StringArrayTypeHandler handler = new StringArrayTypeHandler();

    @Mock
    private PreparedStatement ps;
    @Mock
    private ResultSet rs;
    @Mock
    private Connection conn;
    @Mock
    private Array sqlArray;

    @Test
    void setNonNullParameter_shouldCreateAndSetArray() throws SQLException {
        String[] tags = {"java", "ddd"};
        when(ps.getConnection()).thenReturn(conn);
        when(conn.createArrayOf("text", tags)).thenReturn(sqlArray);

        handler.setNonNullParameter(ps, 1, tags, JdbcType.ARRAY);

        verify(ps).setArray(1, sqlArray);
        verify(sqlArray).free();
    }

    @Test
    void getNullableResult_byColumnName_shouldReturnArray() throws SQLException {
        String[] expected = {"a", "b", "c"};
        when(rs.getArray("tags")).thenReturn(sqlArray);
        when(sqlArray.getArray()).thenReturn(expected);

        String[] result = handler.getNullableResult(rs, "tags");

        assertThat(result).containsExactly("a", "b", "c");
        verify(sqlArray).free();
    }

    @Test
    void getNullableResult_shouldReturnNullWhenArrayNull() throws SQLException {
        when(rs.getArray("tags")).thenReturn(null);

        String[] result = handler.getNullableResult(rs, "tags");

        assertThat(result).isNull();
    }

    @Test
    void getNullableResult_byColumnIndex_shouldReturnArray() throws SQLException {
        String[] expected = {"x"};
        when(rs.getArray(1)).thenReturn(sqlArray);
        when(sqlArray.getArray()).thenReturn(expected);

        String[] result = handler.getNullableResult(rs, 1);

        assertThat(result).containsExactly("x");
    }
}
