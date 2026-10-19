package com.yoursweakfoe.common.exception;

import com.yoursweakfoe.common.exception.handler.GlobalRestExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * 统一异常处理自动装配 —— Spring Boot AutoConfiguration 注册，引入依赖即生效。
 *
 * <p>REST 通道：{@link GlobalRestExceptionHandler}（Servlet Web 应用且存在 DispatcherServlet），
 * 将 BusinessException 统一翻译为 HTTP 422 + RFC 9457 Problem Details。
 */
@AutoConfiguration
public class ExceptionAutoConfiguration {

    /** REST 通道全局异常处理（RFC 9457）。 */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
    public GlobalRestExceptionHandler globalRestExceptionHandler() {
        return new GlobalRestExceptionHandler();
    }
}
