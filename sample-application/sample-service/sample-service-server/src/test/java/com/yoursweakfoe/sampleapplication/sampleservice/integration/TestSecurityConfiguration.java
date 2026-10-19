package com.yoursweakfoe.sampleapplication.sampleservice.integration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * 集成测试安全配置 —— 提供 {@link JwtDecoder} 以满足
 * {@code SecurityAutoConfiguration#securityFilterChain} 的构造要求。
 *
 * <p>测试链路走 {@code permitAll}，实际不触发验签，故用一个恒返回固定 Jwt 的
 * 轻量 decoder 即可，无需真实 JWKS 端点。
 */
@TestConfiguration
public class TestSecurityConfiguration {

    @Bean
    public JwtDecoder jwtDecoder() {
        return token -> Jwt.withTokenValue(token)
                .header("alg", "none")
                .subject("integration-test")
                .build();
    }
}
