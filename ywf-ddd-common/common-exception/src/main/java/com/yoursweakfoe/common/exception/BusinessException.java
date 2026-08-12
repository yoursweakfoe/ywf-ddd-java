package com.yoursweakfoe.common.exception;

import lombok.Getter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 业务异常 —— 所有业务层面错误的统一异常。
 *
 * <p>覆盖场景：业务规则违反、流程状态不合法、外部服务调用失败等。
 * 由全局异常处理捕获并翻译：REST 通道 {@code GlobalRestExceptionHandler}
 * （{@code @RestControllerAdvice}）→ HTTP 422 + RFC 9457。
 *
 * <p><b>生效方式</b>：上述翻译逻辑位于 {@code common-exception} 模块的
 * {@code ExceptionAutoConfiguration}（Spring Boot 自动装配），引入依赖即生效。
 *
 * <p><b>国际化约定</b>：{@code messageKey} 必须是前端 i18n 位点（如 {@code "order:err.insufficientStock"}），
 * 由前端通过 {@code t(key, params)} 渲染本地化文案。禁止使用硬编码的可读文案。
 *
 * <p><b>使用示例：</b>
 * <pre>{@code
 * throw new BusinessException("order:err.insufficientStock",
 *         Map.of("sku", sku, "required", qty, "available", stock));
 * }</pre>
 */
public class BusinessException extends RuntimeException {

    /** i18n 占位符参数，空表示无插值 */
    @Getter
    private final Map<String, Object> params;

    /**
     * 构造函数 —— 仅 i18n 位点，无占位符参数。
     *
     * @param messageKey i18n 位点（如 {@code "order:err.insufficientStock"}）
     */
    public BusinessException(String messageKey) {
        this(messageKey, Collections.emptyMap());
    }

    /**
     * 构造函数 —— i18n 位点 + 占位符参数。
     *
     * @param messageKey i18n 位点（如 {@code "order:err.insufficientStock"}）
     * @param params 占位符参数，{@code null} 视为空 Map
     */
    public BusinessException(String messageKey, Map<String, Object> params) {
        super(messageKey);
        this.params = params == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(params));
    }

}
