package com.yoursweakfoe.common.pg.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@ExtendWith(MockitoExtension.class)
class JsonNodeTypeHandlerTest {

    private final JsonNodeTypeHandler handler = new JsonNodeTypeHandler();
    private static final JsonMapper MAPPER = new JsonMapper();

    @Mock private ResultSet rs;

    @Test
    void getNullableResult_shouldParseJsonToNode() throws SQLException {
        String json = "{\"name\":\"test\",\"value\":42}";
        when(rs.getString("config")).thenReturn(json);

        JsonNode result = handler.getNullableResult(rs, "config");

        assertThat(result).isNotNull();
        assertThat(result.get("name").asText()).isEqualTo("test");
        assertThat(result.get("value").asInt()).isEqualTo(42);
    }

    @Test
    void getNullableResult_shouldReturnNullWhenColumnNull() throws SQLException {
        when(rs.getString("config")).thenReturn(null);

        JsonNode result = handler.getNullableResult(rs, "config");

        assertThat(result).isNull();
    }

    @Test
    void getNullableResult_shouldThrowOnInvalidJson() throws SQLException {
        when(rs.getString("config")).thenReturn("{invalid json!!!");

        assertThatThrownBy(() -> handler.getNullableResult(rs, "config"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("Failed to parse JSON");
    }

    @Test
    void getNullableResult_nestedStructure() throws SQLException {
        String json = "{\"order\":{\"items\":[{\"sku\":\"A\"},{\"sku\":\"B\"}]}}";
        when(rs.getString("data")).thenReturn(json);

        JsonNode result = handler.getNullableResult(rs, "data");

        assertThat(result.get("order").get("items")).hasSize(2);
        assertThat(result.get("order").get("items").get(0).get("sku").asText()).isEqualTo("A");
    }

    @Test
    void setNonNullParameter_shouldSerializeNode() throws SQLException {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("key", "value");

        var ps = org.mockito.Mockito.mock(java.sql.PreparedStatement.class);
        handler.setNonNullParameter(ps, 1, node, org.apache.ibatis.type.JdbcType.OTHER);

        org.mockito.Mockito.verify(ps).setObject(org.mockito.ArgumentMatchers.eq(1),
                org.mockito.ArgumentMatchers.argThat(obj -> {
                    org.postgresql.util.PGobject pg = (org.postgresql.util.PGobject) obj;
                    return "jsonb".equals(pg.getType()) && pg.getValue().contains("\"key\"");
                }));
    }
}
