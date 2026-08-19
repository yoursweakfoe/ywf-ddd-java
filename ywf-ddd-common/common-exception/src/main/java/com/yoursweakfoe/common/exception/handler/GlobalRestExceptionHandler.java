package com.yoursweakfoe.common.exception.handler;

import com.yoursweakfoe.common.exception.type.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * REST 通道全局异常处理器（{@code @RestControllerAdvice}）。
 *
 * <p>在 Spring MVC 管线中将领域异常翻译为符合 <strong>RFC 9457</strong>（Problem Details for HTTP APIs）
 * 标准的 HTTP 错误响应，Content-Type 为 {@code application/problem+json}。
 *
 * <p>响应格式：
 * <pre>
 * {
 *   "type": "about:blank",
 *   "title": "Business Error",
 *   "status": 422,
 *   "detail": "order:err.insufficientStock",
 *   "instance": "/api/orders",
 *   "params": { "sku": "A001", "required": 10, "available": 3 }
 * }
 * </pre>
 *
 * <p>字段语义（RFC 9457 §3.2）：
 * <ul>
 *   <li>{@code type} — 错误类别 URI（当前为 {@code about:blank}，待错误类型文档化后可替换为绝对 URI）
 *   <li>{@code title} — 人类可读的简短摘要
 *   <li>{@code status} — HTTP 状态码
 *   <li>{@code detail} — 本次错误的具体描述（此处为 i18n messageKey）
 *   <li>{@code instance} — 标识本次具体发生的请求路径
 *   <li>{@code params} / {@code fieldErrors} — 合规扩展字段（RFC 允许添加自定义成员）
 * </ul>
 *
 * <p>由 {@code ExceptionAutoConfiguration} 在 Servlet Web 应用中注册为 Bean，
 * Spring MVC 自动发现 {@code @RestControllerAdvice} 并接入异常解析管线。
 */
@Slf4j
@RestControllerAdvice
public class GlobalRestExceptionHandler {

    /** RFC 9457 标准媒体类型 */
    private static final MediaType CONTENT_TYPE_PROBLEM =
            MediaType.parseMediaType("application/problem+json");

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessException e, HttpServletRequest request) {
        log.warn("Business error: {} | params: {}", e.getMessage(), e.getParams());
        Map<String, Object> body = problemBody(request, "Business Error", 422, e.getMessage());
        if (!e.getParams().isEmpty()) {
            body.put("params", e.getParams());
        }
        return problem(body, 422);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            ConstraintViolationException e, HttpServletRequest request) {
        log.warn("Validation failed: {}", e.getMessage());
        Map<String, String> fieldErrors = e.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        v -> v.getMessage(),
                        (a, b) -> a));
        Map<String, Object> body = problemBody(request, "Validation Failed", 400, "Parameter validation failed");
        body.put("fieldErrors", fieldErrors);
        return problem(body, 400);
    }

    /**
     * MVC 参数绑定校验失败抛出的异常类型
     * （语义对齐 ConstraintViolationException 处理）。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        log.warn("Validation failed: {}", e.getMessage());
        Map<String, String> fieldErrors = e.getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fe -> fe.getField(),
                        fe -> fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage(),
                        (a, b) -> a));
        Map<String, Object> body = problemBody(request, "Validation Failed", 400, "Parameter validation failed");
        body.put("fieldErrors", fieldErrors);
        return problem(body, 400);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException e, HttpServletRequest request) {
        log.warn("Illegal state: {}", e.getMessage());
        return problem(problemBody(request, "Conflict", 409, e.getMessage()), 409);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("Bad request: {}", e.getMessage(), e);
        return problem(problemBody(request, "Bad Request", 400, e.getMessage()), 400);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnknown(Exception e, HttpServletRequest request) {
        log.error("Unhandled exception in REST pipeline", e);
        return problem(problemBody(request, "Internal Server Error", 500, "Internal Server Error"), 500);
    }

    // ==================== 工具方法 ====================

    /** 构建完整 RFC 9457 响应（含 Content-Type 头） */
    private static ResponseEntity<Map<String, Object>> problem(Map<String, Object> body, int status) {
        return ResponseEntity.status(status)
                .contentType(CONTENT_TYPE_PROBLEM)
                .body(body);
    }

    /** 构建 RFC 9457 响应体（type / title / status / detail / instance + 可扩展） */
    private static Map<String, Object> problemBody(HttpServletRequest request,
                                                   String title, int status, String detail) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", "about:blank");
        map.put("title", title);
        map.put("status", status);
        map.put("detail", detail);
        map.put("instance", request != null ? request.getRequestURI() : null);
        return map;
    }
}
