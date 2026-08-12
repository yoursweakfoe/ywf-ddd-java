package com.yoursweakfoe.common.pg.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UUIDArrayTypeHandlerTest {

    private final UUIDArrayTypeHandler handler = new UUIDArrayTypeHandler();

    @Mock private PreparedStatement ps;
    @Mock private ResultSet rs;
    @Mock private Connection conn;
    @Mock private Array sqlArray;

    @Test
    void setNonNullParameter_shouldCreateUuidArray() throws SQLException {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID[] values = {id1, id2};
        when(ps.getConnection()).thenReturn(conn);
        when(conn.createArrayOf("uuid", values)).thenReturn(sqlArray);

        handler.setNonNullParameter(ps, 1, values, JdbcType.ARRAY);

        verify(ps).setArray(1, sqlArray);
        verify(sqlArray).free();
    }

    @Test
    void getNullableResult_shouldReturnArray() throws SQLException {
        UUID id = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID[] expected = {id};
        when(rs.getArray("related_ids")).thenReturn(sqlArray);
        when(sqlArray.getArray()).thenReturn(expected);

        UUID[] result = handler.getNullableResult(rs, "related_ids");

        assertThat(result).containsExactly(id);
    }

    @Test
    void getNullableResult_shouldReturnNullWhenArrayNull() throws SQLException {
        when(rs.getArray("related_ids")).thenReturn(null);

        assertThat(handler.getNullableResult(rs, "related_ids")).isNull();
    }
}
