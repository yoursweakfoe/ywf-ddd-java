package com.yoursweakfoe.common.cloud.feign;

import com.yoursweakfoe.common.cloud.feign.interceptor.JwtPropagationRequestInterceptor;
import feign.RequestInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * 东西向 JWT 传播自动装配 —— 注册 {@link JwtPropagationRequestInterceptor}。
 *
 * <p>Spring Cloud OpenFeign 会自动把所有 {@link RequestInterceptor} Bean 应用到
 * 每个 Feign 客户端，因此只需注册 Bean 即可，无需逐个客户端配置。
 *
 * <p>门控条件：仅当 classpath 同时存在 Feign（{@link RequestInterceptor}）与
 * Spring Security OAuth2 JWT（{@link Jwt}）时激活——JWT 不存在（如仅 Feign 无
 * common-security 的极简消费方）时跳过，避免 NoClassDefFoundError。
 */
@AutoConfiguration
@ConditionalOnClass({RequestInterceptor.class, Jwt.class})
@EnableConfigurationProperties(CommonCloudProperties.class)
public class FeignJwtPropagationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtPropagationRequestInterceptor jwtPropagationRequestInterceptor(
            CommonCloudProperties properties) {
        return new JwtPropagationRequestInterceptor(properties);
    }
}
