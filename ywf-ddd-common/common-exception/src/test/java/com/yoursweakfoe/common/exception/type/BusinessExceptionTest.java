package com.yoursweakfoe.common.exception.type;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BusinessException — 业务异常测试")
class BusinessExceptionTest {

    @Test
    void constructor_messageKeyOnly() {
        BusinessException ex = new BusinessException("order:err.notFound");
        assertThat(ex.getMessage()).isEqualTo("order:err.notFound");
        assertThat(ex.getParams()).isEmpty();
    }

    @Test
    void constructor_withParams() {
        Map<String, Object> params = Map.of("sku", "ABC", "qty", 5);
        BusinessException ex = new BusinessException("order:err.insufficientStock", params);
        assertThat(ex.getMessage()).isEqualTo("order:err.insufficientStock");
        assertThat(ex.getParams())
                .containsEntry("sku", "ABC")
                .containsEntry("qty", 5);
    }

    @Test
    void getMessage_returnsMessageKey() {
        BusinessException ex = new BusinessException("product:err.notFound");
        assertThat(ex.getMessage()).isEqualTo("product:err.notFound");
    }

    @Test
    void getParams_nullParams_returnsEmptyMap() {
        BusinessException ex = new BusinessException("order:err.key", null);
        assertThat(ex.getParams()).isNotNull().isEmpty();
    }

    @Test
    void getParams_immutable() {
        Map<String, Object> params = Map.of("key", "value");
        BusinessException ex = new BusinessException("order:err.key", params);
        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> ex.getParams().put("new", "value"));
    }

    @Test
    void isRuntimeException() {
        BusinessException ex = new BusinessException("test:err.key");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    void constructor_withHttpStatus() {
        BusinessException ex = new BusinessException("order:err.notFound", 404);
        assertThat(ex.getMessage()).isEqualTo("order:err.notFound");
        assertThat(ex.getHttpStatus()).isEqualTo(404);
        assertThat(ex.getParams()).isEmpty();
    }

    @Test
    void constructor_withParamsAndHttpStatus() {
        BusinessException ex = new BusinessException("order:err.alreadyConfirmed",
                Map.of("id", "A001"), 409);
        assertThat(ex.getHttpStatus()).isEqualTo(409);
        assertThat(ex.getParams()).containsEntry("id", "A001");
    }

    @Test
    void getHttpStatus_nullWhenNotSpecified() {
        BusinessException ex = new BusinessException("order:err.notFound");
        assertThat(ex.getHttpStatus()).isNull();

        BusinessException exWithParams = new BusinessException("order:err.key", Map.of("k", "v"));
        assertThat(exWithParams.getHttpStatus()).isNull();
    }
}
