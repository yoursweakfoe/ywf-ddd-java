package com.yoursweakfoe.common.exception.handler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yoursweakfoe.common.exception.type.BusinessException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST 通道全局异常处理测试 —— MockMvc standalone 管线（真实 DispatcherServlet
 * 异常解析 + ControllerAdvice），验证 RFC 9457 响应格式与迁移前语义一致。
 */
@DisplayName("GlobalRestExceptionHandler — REST 通道 RFC 9457")
class GlobalRestExceptionHandlerTest {

    private static final MediaType PROBLEM_JSON = MediaType.parseMediaType("application/problem+json");

    private MockMvc mockMvc;

    /** 抛错端点 fixture（覆盖全部映射分支）。 */
    @RestController
    static class TestEndpoints {

        @GetMapping("/business")
        public String business() {
            throw new BusinessException("order:err.outOfStock");
        }

        @GetMapping("/business-params")
        public String businessParams() {
            throw new BusinessException("order:err.insufficientStock",
                    Map.of("sku", "A001", "required", 10));
        }

        @GetMapping("/constraint-violation")
        public String constraintViolation() {
            Path path = mock(Path.class);
            when(path.toString()).thenReturn("name");
            @SuppressWarnings("unchecked")
            ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
            when(violation.getPropertyPath()).thenReturn(path);
            when(violation.getMessage()).thenReturn("must not be blank");
            throw new ConstraintViolationException("validation failed", Set.of(violation));
        }

        @GetMapping("/illegal-state")
        public String illegalState() {
            throw new IllegalStateException("Order already confirmed");
        }

        @GetMapping("/illegal-argument")
        public String illegalArgument() {
            throw new IllegalArgumentException("quantity must be positive");
        }

        @GetMapping("/unknown")
        public String unknown() {
            throw new RuntimeException("unexpected failure");
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestEndpoints())
                .setControllerAdvice(new GlobalRestExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("BusinessException → 422 RFC 9457")
    void businessException_returns422Rfc9457() throws Exception {
        mockMvc.perform(get("/business"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Business Error"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.detail").value("order:err.outOfStock"))
                .andExpect(jsonPath("$.instance").value("/business"));
    }

    @Test
    @DisplayName("BusinessException 携带 params → 响应包含 params 扩展字段")
    void businessExceptionWithParams_includesParamsInResponse() throws Exception {
        mockMvc.perform(get("/business-params"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Business Error"))
                .andExpect(jsonPath("$.detail").value("order:err.insufficientStock"))
                .andExpect(jsonPath("$.params.sku").value("A001"))
                .andExpect(jsonPath("$.params.required").value(10));
    }

    @Test
    @DisplayName("ConstraintViolationException → 400 + fieldErrors")
    void constraintViolation_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(get("/constraint-violation"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value("/constraint-violation"))
                .andExpect(jsonPath("$.fieldErrors.name").value("must not be blank"));
    }

    @Test
    @DisplayName("IllegalStateException → 409")
    void illegalState_returns409() throws Exception {
        mockMvc.perform(get("/illegal-state"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value("Order already confirmed"))
                .andExpect(jsonPath("$.instance").value("/illegal-state"));
    }

    @Test
    @DisplayName("IllegalArgumentException → 400")
    void illegalArgument_returns400() throws Exception {
        mockMvc.perform(get("/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("quantity must be positive"));
    }

    @Test
    @DisplayName("未知异常 → 500（原始信息不落响应体）")
    void unknownException_returns500() throws Exception {
        mockMvc.perform(get("/unknown"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("Internal Server Error"));
    }
}
