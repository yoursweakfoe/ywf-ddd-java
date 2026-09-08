package com.yoursweakfoe.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@DisplayName("SecurityAutoConfiguration — opt-out 门控")
class SecurityOptOutTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(JwtDecoderConfig.class)
            .withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class));

    @Configuration
    static class JwtDecoderConfig {
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .subject("test")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(300))
                    .build();
        }
    }

    @Test
    @DisplayName("缺省（不写 enabled）→ 安全链注册（向后兼容）")
    void defaultEnabled_securityFilterChainPresent() {
        contextRunner.run(context -> {
            assertThat(context.getBeansOfType(SecurityFilterChain.class)).isNotEmpty();
        });
    }

    @Test
    @DisplayName("ywf.security.enabled=false → 无 SecurityFilterChain bean")
    void disabled_securityFilterChainAbsent() {
        contextRunner
                .withPropertyValues("ywf.security.enabled=false")
                .run(context -> {
                    assertThat(context.getBeansOfType(SecurityFilterChain.class)).isEmpty();
                });
    }
}