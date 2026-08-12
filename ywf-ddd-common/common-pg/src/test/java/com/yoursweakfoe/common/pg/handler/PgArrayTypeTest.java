package com.yoursweakfoe.common.pg.handler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PgArrayTypeTest {

    @Test
    void shouldHaveAllExpectedValues() {
        assertThat(PgArrayType.values()).hasSize(8);
    }

    @Test
    void textShouldMapToTextType() {
        assertThat(PgArrayType.TEXT.getPgTypeName()).isEqualTo("text");
    }

    @Test
    void integerShouldMapToIntegerType() {
        assertThat(PgArrayType.INTEGER.getPgTypeName()).isEqualTo("integer");
    }

    @Test
    void bigintShouldMapToBigintType() {
        assertThat(PgArrayType.BIGINT.getPgTypeName()).isEqualTo("bigint");
    }

    @Test
    void int2ShouldMapToInt2Type() {
        assertThat(PgArrayType.INT2.getPgTypeName()).isEqualTo("int2");
    }

    @Test
    void float4ShouldMapToRealType() {
        assertThat(PgArrayType.FLOAT4.getPgTypeName()).isEqualTo("float4");
    }

    @Test
    void float8ShouldMapToDoublePrecisionType() {
        assertThat(PgArrayType.FLOAT8.getPgTypeName()).isEqualTo("float8");
    }

    @Test
    void booleanShouldMapToBooleanType() {
        assertThat(PgArrayType.BOOLEAN.getPgTypeName()).isEqualTo("boolean");
    }

    @Test
    void uuidShouldMapToUuidType() {
        assertThat(PgArrayType.UUID.getPgTypeName()).isEqualTo("uuid");
    }

    @Test
    void valueOfShouldWorkForAllConstants() {
        assertThat(PgArrayType.valueOf("TEXT")).isEqualTo(PgArrayType.TEXT);
        assertThat(PgArrayType.valueOf("INTEGER")).isEqualTo(PgArrayType.INTEGER);
        assertThat(PgArrayType.valueOf("BIGINT")).isEqualTo(PgArrayType.BIGINT);
        assertThat(PgArrayType.valueOf("UUID")).isEqualTo(PgArrayType.UUID);
    }
}
