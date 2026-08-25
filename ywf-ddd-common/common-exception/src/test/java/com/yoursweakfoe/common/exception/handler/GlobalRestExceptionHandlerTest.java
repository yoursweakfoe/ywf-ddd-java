package com.yoursweakfoe.common.exception.handler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST 通道全局异常处理测试 —— MockMvc standalone 管线（真实 DispatcherServlet
 * 异常解析 + ControllerAdvice），验证 RFC 9457 响应格式。
 *
 * <p>安全契约：技术类异常（IllegalState/IllegalArgument/TypeMismatch）的 detail
 * 为稳定泛化文案，不回显原始异常消息——原始信息只进服务端日志。
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

        @GetMapping("/business-404")
        public String business404() {
            throw new BusinessException("order:err.notFound", 404);
        }

        @GetMapping("/business-409")
        public String business409() {
            throw new BusinessException("order:err.alreadyConfirmed",
                    Map.of("id", "A001"), 409);
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

        @GetMapping("/type-mismatch/{id}")
        public String typeMismatch(@org.springframework.web.bind.annotation.PathVariable java.util.UUID id) {
            return id.toString();
        }

        @PostMapping("/echo")
        public String echo(@RequestBody Map<String, Object> body) {
            return body.toString();
        }

        @GetMapping("/needs-param")
        public String needsParam(@RequestParam String q) {
            return q;
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
    @DisplayName("BusinessException 显式 404 → 响应 404")
    void businessExceptionWithExplicit404() throws Exception {
        mockMvc.perform(get("/business-404"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Business Error"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("order:err.notFound"))
                .andExpect(jsonPath("$.instance").value("/business-404"));
    }

    @Test
    @DisplayName("BusinessException 显式 409 + params → 响应 409")
    void businessExceptionWithExplicit409AndParams() throws Exception {
        mockMvc.perform(get("/business-409"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("order:err.alreadyConfirmed"))
                .andExpect(jsonPath("$.params.id").value("A001"));
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
    @DisplayName("IllegalStateException → 409（detail 稳定泛化，不回显原始消息）")
    void illegalState_returns409() throws Exception {
        mockMvc.perform(get("/illegal-state"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value("Conflict"))
                .andExpect(jsonPath("$.instance").value("/illegal-state"));
    }

    @Test
    @DisplayName("IllegalArgumentException → 400（detail 稳定泛化，不回显原始消息）")
    void illegalArgument_returns400() throws Exception {
        mockMvc.perform(get("/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Bad Request"));
    }

    @Test
    @DisplayName("路径变量类型转换失败（非法 UUID）→ 400 而非兜底 500（detail 不含原始值）")
    void typeMismatch_returns400WithParamName() throws Exception {
        mockMvc.perform(get("/type-mismatch/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Parameter 'id' must be of type UUID"))
                .andExpect(jsonPath("$.instance").value("/type-mismatch/not-a-uuid"));
    }

    @Test
    @DisplayName("JSON 请求体畸形 → 400 而非兜底 500")
    void malformedBody_returns400() throws Exception {
        mockMvc.perform(post("/echo")
                        .contentType(APPLICATION_JSON)
                        .content("{invalid json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Malformed request body"));
    }

    @Test
    @DisplayName("缺失必填参数 → 400 而非兜底 500")
    void missingParameter_returns400() throws Exception {
        mockMvc.perform(get("/needs-param"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Missing required parameter 'q'"));
    }

    @Test
    @DisplayName("HTTP 方法不支持 → 405 而非兜底 500")
    void methodNotSupported_returns405() throws Exception {
        mockMvc.perform(get("/echo"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentType(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Method Not Allowed"));
    }

    @Test
    @DisplayName("媒体类型不支持 → 415 而非兜底 500")
    void mediaTypeNotSupported_returns415() throws Exception {
        mockMvc.perform(post("/echo")
                        .contentType(TEXT_PLAIN)
                        .content("plain text body"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentType(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Unsupported Media Type"));
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
