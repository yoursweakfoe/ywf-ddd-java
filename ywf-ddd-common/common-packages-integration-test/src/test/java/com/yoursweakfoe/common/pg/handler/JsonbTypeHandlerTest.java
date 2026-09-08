package com.yoursweakfoe.common.pg.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.postgresql.util.PGobject;

@ExtendWith(MockitoExtension.class)
class JsonbTypeHandlerTest {

    private final JsonbTypeHandler handler = new JsonbTypeHandler();

    @Mock private PreparedStatement ps;
    @Mock private ResultSet rs;

    @Test
    void setNonNullParameter_shouldSetPGobjectWithJsonbType() throws SQLException {
        String json = "{\"key\":\"value\"}";

        handler.setNonNullParameter(ps, 1, json, JdbcType.OTHER);

        verify(ps).setObject(org.mockito.ArgumentMatchers.eq(1), argThat(obj -> {
            PGobject pg = (PGobject) obj;
            return "jsonb".equals(pg.getType()) && json.equals(pg.getValue());
        }));
    }

    @Test
    void getNullableResult_byColumnName_shouldReturnJsonString() throws SQLException {
        String json = "{\"items\":[1,2,3]}";
        when(rs.getString("metadata")).thenReturn(json);

        String result = handler.getNullableResult(rs, "metadata");

        assertThat(result).isEqualTo(json);
    }

    @Test
    void getNullableResult_byColumnIndex_shouldReturnJsonString() throws SQLException {
        String json = "[\"a\",\"b\"]";
        when(rs.getString(1)).thenReturn(json);

        String result = handler.getNullableResult(rs, 1);

        assertThat(result).isEqualTo(json);
    }

    @Test
    void getNullableResult_shouldReturnNullWhenColumnNull() throws SQLException {
        when(rs.getString("metadata")).thenReturn(null);

        String result = handler.getNullableResult(rs, "metadata");

        assertThat(result).isNull();
    }

    @Test
    void setNonNullParameter_complexNestedJson() throws SQLException {
        String json = "{\"order\":{\"id\":\"abc\",\"items\":[{\"sku\":\"A001\",\"qty\":2}]}}";

        handler.setNonNullParameter(ps, 1, json, JdbcType.OTHER);

        verify(ps).setObject(org.mockito.ArgumentMatchers.eq(1), argThat(obj -> {
            PGobject pg = (PGobject) obj;
            return json.equals(pg.getValue());
        }));
    }
}
