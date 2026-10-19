package com.yoursweakfoe.common.exception.handler;

import com.yoursweakfoe.common.exception.type.BusinessException;
import com.yoursweakfoe.common.exception.type.SilentWriteLossException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * REST 通道全局异常处理器（{@code @RestControllerAdvice}）。
 *
 * <p>将异常统一翻译为符合 <strong>RFC 9457</strong>（Problem Details for HTTP APIs）
 * 标准的 HTTP 错误响应，载体使用 Spring 内建 {@link ProblemDetail}，
 * Content-Type 为 {@code application/problem+json}。
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
 *   <li>{@code type} — 错误类别 URI（当前为 about:blank，待错误类型文档化后可替换为绝对 URI）
 *   <li>{@code title} / {@code status} / {@code detail} / {@code instance} — RFC 标准成员
 *   <li>{@code params} / {@code fieldErrors} — 合规扩展成员（{@link ProblemDetail#addProperty}）
 * </ul>
 *
 * <h3>安全约束：detail 不回显内部信息</h3>
 * <p>仅 BusinessException 的 detail 携带 i18n messageKey（对外契约，前端 t(key, params) 渲染）；
 * 技术类异常（IllegalState / IllegalArgument 等）的 detail 一律为<strong>稳定泛化文案</strong>，
 * 原始异常消息只进服务端日志——防止 SQL 片段、实体 ID 等实现细节经响应体外泄。
 *
 * <h3>框架客户端异常显式映射表</h3>
 * <p>Spring 中 {@code ExceptionHandlerExceptionResolver} 先于
 * {@code DefaultHandlerExceptionResolver} 执行——若不显式映射下列框架异常，
 * 它们会全部落入 {@code Exception} 兜底变成 500 + ERROR 假告警：
 *
 * <table>
 *   <tr><th>异常</th><th>状态</th><th>场景</th></tr>
 *   <tr><td>HttpMessageNotReadableException</td><td>400</td><td>请求体不可读 / JSON 畸形</td></tr>
 *   <tr><td>BindException（含 MethodArgumentNotValidException 子类）</td><td>400</td><td>参数绑定 / 校验失败</td></tr>
 *   <tr><td>MissingServletRequestParameterException</td><td>400</td><td>缺失必填参数</td></tr>
 *   <tr><td>MethodArgumentTypeMismatchException</td><td>400</td><td>参数类型转换失败</td></tr>
 *   <tr><td>ConstraintViolationException</td><td>400</td><td>约束校验失败</td></tr>
 *   <tr><td>NoResourceFoundException</td><td>404</td><td>请求资源不存在</td></tr>
 *   <tr><td>HttpRequestMethodNotSupportedException</td><td>405</td><td>HTTP 方法不支持</td></tr>
 *   <tr><td>HttpMediaTypeNotSupportedException</td><td>415</td><td>媒体类型不支持</td></tr>
 *   <tr><td>IllegalStateException</td><td>409</td><td>状态冲突（含乐观锁冲突）</td></tr>
 *   <tr><td>SilentWriteLossException</td><td>500 + ERROR 日志</td><td>写丢失级不可能状态（INSERT/DELETE 0 影响行），显式告警通道</td></tr>
 *   <tr><td>BusinessException</td><td>业务指定或缺省 422</td><td>领域规则违反</td></tr>
 *   <tr><td>Exception（兜底）</td><td>500</td><td>未预期异常，泛化标题不泄内部信息</td></tr>
 * </table>
 *
 * <p>由 {@code ExceptionAutoConfiguration} 在 Servlet Web 应用中注册为 Bean，
 * Spring MVC 自动发现 {@code @RestControllerAdvice} 并接入异常解析管线。
 */
@Slf4j
@RestControllerAdvice
public class GlobalRestExceptionHandler {

    /** RFC 9457 标准媒体类型（显式声明，保证 standalone MockMvc 与生产管线行为一致） */
    private static final MediaType CONTENT_TYPE_PROBLEM =
            MediaType.parseMediaType("application/problem+json");

    /** RFC 9457 标准成员 {@code type} 的默认值（未文档化错误类型前统一为 about:blank） */
    private static final URI TYPE_ABOUT_BLANK = URI.create("about:blank");

    /** 各类异常的 RFC 9457 {@code title} 摘要（常量收敛，避免散落字面量） */
    private static final String TITLE_BUSINESS = "Business Error";
    private static final String TITLE_VALIDATION = "Validation Failed";
    private static final String TITLE_NOT_FOUND = "Not Found";
    private static final String TITLE_CONFLICT = "Conflict";
    private static final String TITLE_BAD_REQUEST = "Bad Request";
    private static final String TITLE_METHOD_NOT_ALLOWED = "Method Not Allowed";
    private static final String TITLE_UNSUPPORTED_MEDIA_TYPE = "Unsupported Media Type";
    private static final String TITLE_INTERNAL_ERROR = "Internal Server Error";

    /**
     * 技术类异常对外暴露的稳定泛化 detail——原始异常消息只进服务端日志（见类级安全约束说明）。
     */
    private static final String DETAIL_CONFLICT = "Conflict";
    private static final String DETAIL_BAD_REQUEST = "Bad Request";

    /** BusinessException 未显式指定状态时的缺省状态码 */
    private static final int DEFAULT_BUSINESS_STATUS = 422;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusiness(BusinessException e, HttpServletRequest request) {
        int status = e.getHttpStatus() != null ? e.getHttpStatus() : DEFAULT_BUSINESS_STATUS;
        log.warn("Business error: {} | params: {}", e.getMessage(), e.getParams());
        ProblemDetail body = problemDetail(request, TITLE_BUSINESS, status, e.getMessage());
        if (!e.getParams().isEmpty()) {
            body.setProperty("params", e.getParams());
        }
        return problem(body, status);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException e, HttpServletRequest request) {
        log.warn("Validation failed: {}", e.getMessage());
        Map<String, String> fieldErrors = e.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        v -> v.getMessage(),
                        (a, b) -> a,
                        LinkedHashMap::new));
        return fieldValidationProblem(request, fieldErrors);
    }

    /**
     * MVC {@code @RequestBody} 参数校验失败。
     * MethodArgumentNotValidException 是 BindException 的子类，Spring 优先匹配本 handler；
     * 纯绑定失败由下方 {@link #handleBind} 承接。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        log.warn("Validation failed: {}", e.getMessage());
        return fieldValidationProblem(request, fieldErrorsOf(e.getFieldErrors()));
    }

    /**
     * MVC 参数绑定失败（如 GET 分页参数 {@code pageNum=abc} 类型转换错误）。
     * 若无本 handler，该框架级客户端错误会落入兜底变 500。
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ProblemDetail> handleBind(BindException e, HttpServletRequest request) {
        log.warn("Binding failed: {}", e.getMessage());
        return fieldValidationProblem(request, fieldErrorsOf(e.getFieldErrors()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleMessageNotReadable(
            HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn("Malformed request body: {}", e.getMessage());
        return problem(problemDetail(request, TITLE_BAD_REQUEST, HttpStatus.BAD_REQUEST.value(),
                "Malformed request body"), HttpStatus.BAD_REQUEST.value());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingParameter(
            MissingServletRequestParameterException e, HttpServletRequest request) {
        log.warn("Missing parameter: {}", e.getParameterName());
        return problem(problemDetail(request, TITLE_BAD_REQUEST, HttpStatus.BAD_REQUEST.value(),
                "Missing required parameter '%s'".formatted(e.getParameterName())),
                HttpStatus.BAD_REQUEST.value());
    }

    /**
     * 路径变量 / 请求参数类型转换失败（如非法 UUID、错误枚举名、非数字格式）。
     * detail 只含参数名与期望类型，不含原始值（避免回显客户端输入）。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(
            MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        log.warn("Type mismatch: {}", e.getMessage());
        Class<?> requiredType = e.getRequiredType();
        String typeName = requiredType != null ? requiredType.getSimpleName() : "expected type";
        return problem(problemDetail(request, TITLE_BAD_REQUEST, HttpStatus.BAD_REQUEST.value(),
                "Parameter '%s' must be of type %s".formatted(e.getName(), typeName)),
                HttpStatus.BAD_REQUEST.value());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResourceFound(
            NoResourceFoundException e, HttpServletRequest request) {
        log.debug("Resource not found: {}", request != null ? request.getRequestURI() : null);
        return problem(problemDetail(request, TITLE_NOT_FOUND, HttpStatus.NOT_FOUND.value(),
                "Resource not found"), HttpStatus.NOT_FOUND.value());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        log.warn("Method not supported: {}", e.getMessage());
        return problem(problemDetail(request, TITLE_METHOD_NOT_ALLOWED, HttpStatus.METHOD_NOT_ALLOWED.value(),
                "HTTP method not supported"), HttpStatus.METHOD_NOT_ALLOWED.value());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        log.warn("Media type not supported: {}", e.getMessage());
        return problem(problemDetail(request, TITLE_UNSUPPORTED_MEDIA_TYPE, HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                "Unsupported media type"), HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());
    }

    /**
     * 状态冲突（乐观锁冲突等）。409 detail 为稳定泛化文案——原始消息可能携带
     * 实体 ID / SQL 片段等内部信息，只记服务端日志。
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleIllegalState(IllegalStateException e, HttpServletRequest request) {
        log.warn("Illegal state: {}", e.getMessage());
        return problem(problemDetail(request, TITLE_CONFLICT, HttpStatus.CONFLICT.value(), DETAIL_CONFLICT),
                HttpStatus.CONFLICT.value());
    }

    /**
     * 静默写丢失（INSERT/DELETE 影响 0 行的不可能状态）——显式告警通道：ERROR 日志（带栈，
     * 运维告警的抓取信号）+ 对外泛化 500。刻意置于 ISE 的 409 通道之前说明分界：
     * 写丢失重试无意义、必须吵醒人，不得混入按 WARN 记账的状态冲突类。
     * 原始消息（含实体 ID / SQL 语义字样）只进日志，响应回稳定泛化文案。
     */
    @ExceptionHandler(SilentWriteLossException.class)
    public ResponseEntity<ProblemDetail> handleSilentWriteLoss(SilentWriteLossException e,
                                                               HttpServletRequest request) {
        log.error("Silent write loss: {}", e.getMessage(), e);
        return problem(problemDetail(request, TITLE_INTERNAL_ERROR,
                HttpStatus.INTERNAL_SERVER_ERROR.value(), TITLE_INTERNAL_ERROR),
                HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("Bad request: {}", e.getMessage(), e);
        return problem(problemDetail(request, TITLE_BAD_REQUEST, HttpStatus.BAD_REQUEST.value(), DETAIL_BAD_REQUEST),
                HttpStatus.BAD_REQUEST.value());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnknown(Exception e, HttpServletRequest request) {
        log.error("Unhandled exception in REST pipeline", e);
        return problem(problemDetail(request, TITLE_INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value(),
                TITLE_INTERNAL_ERROR), HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    // ==================== 工具方法 ====================

    /** 构建 RFC 9457 响应（含 Content-Type 头） */
    private static ResponseEntity<ProblemDetail> problem(ProblemDetail body, int status) {
        return ResponseEntity.status(status)
                .contentType(CONTENT_TYPE_PROBLEM)
                .body(body);
    }

    /** 构建 RFC 9457 ProblemDetail（type / title / status / detail / instance） */
    private static ProblemDetail problemDetail(HttpServletRequest request, String title, int status, String detail) {
        ProblemDetail pd = ProblemDetail.forStatus(status);
        pd.setType(TYPE_ABOUT_BLANK);
        pd.setTitle(title);
        pd.setDetail(detail);
        URI instance = toInstanceUri(request);
        if (instance != null) {
            pd.setInstance(instance);
        }
        return pd;
    }

    /** 校验/绑定失败的公共响应体（400 + fieldErrors 扩展成员） */
    private static ResponseEntity<ProblemDetail> fieldValidationProblem(HttpServletRequest request,
                                                                        Map<String, String> fieldErrors) {
        ProblemDetail body = problemDetail(request, TITLE_VALIDATION, HttpStatus.BAD_REQUEST.value(),
                "Parameter validation failed");
        body.setProperty("fieldErrors", fieldErrors);
        return problem(body, HttpStatus.BAD_REQUEST.value());
    }

    private static Map<String, String> fieldErrorsOf(java.util.List<FieldError> fieldErrors) {
        return fieldErrors.stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage(),
                        (a, b) -> a,
                        LinkedHashMap::new));
    }

    /** 请求 URI 转 instance；畸形 URI（攻击面输入）降级为 null——绝不让异常处理自身抛出二次异常 */
    private static URI toInstanceUri(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String uri = request.getRequestURI();
        if (uri == null || uri.isBlank()) {
            return null;
        }
        try {
            return URI.create(uri);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
