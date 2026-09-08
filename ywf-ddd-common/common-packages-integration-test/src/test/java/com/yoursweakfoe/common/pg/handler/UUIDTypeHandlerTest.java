package com.yoursweakfoe.common.pg.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * UUIDTypeHandler 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class UUIDTypeHandlerTest {

    private final UUIDTypeHandler handler = new UUIDTypeHandler();

    @Mock
    private PreparedStatement ps;
    @Mock
    private ResultSet rs;

    @Test
    void setNonNullParameter_shouldSetObject() throws SQLException {
        UUID uuid = UUID.randomUUID();

        handler.setNonNullParameter(ps, 1, uuid, JdbcType.OTHER);

        verify(ps).setObject(1, uuid);
    }

    @Test
    void getNullableResult_byColumnName_shouldReturnUUID() throws SQLException {
        UUID expected = UUID.randomUUID();
        when(rs.getObject("id")).thenReturn(expected);

        UUID result = handler.getNullableResult(rs, "id");

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getNullableResult_byColumnIndex_shouldReturnUUID() throws SQLException {
        UUID expected = UUID.randomUUID();
        when(rs.getObject(1)).thenReturn(expected);

        UUID result = handler.getNullableResult(rs, 1);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getNullableResult_shouldReturnNullWhenColumnNull() throws SQLException {
        when(rs.getObject("id")).thenReturn(null);

        UUID result = handler.getNullableResult(rs, "id");

        assertThat(result).isNull();
    }
}
