package com.yoursweakfoe.common.cloud.feign;

import com.yoursweakfoe.common.cloud.feign.interceptor.JwtPropagationRequestInterceptor;
import feign.RequestInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 东西向 JWT 传播自动装配 —— 注册 {@link JwtPropagationRequestInterceptor}。
 *
 * <p>Spring Cloud OpenFeign 会自动把所有 {@link RequestInterceptor} Bean 应用到
 * 每个 Feign 客户端，因此只需注册 Bean 即可，无需逐个客户端配置。
 */
@AutoConfiguration
@ConditionalOnClass(RequestInterceptor.class)
public class FeignJwtPropagationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtPropagationRequestInterceptor jwtPropagationRequestInterceptor() {
        return new JwtPropagationRequestInterceptor();
    }
}
