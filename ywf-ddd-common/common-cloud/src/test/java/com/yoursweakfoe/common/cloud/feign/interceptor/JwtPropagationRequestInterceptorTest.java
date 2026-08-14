package com.yoursweakfoe.common.cloud.feign.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import feign.RequestTemplate;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@DisplayName("JwtPropagationRequestInterceptor — 东西向 JWT 透传")
class JwtPropagationRequestInterceptorTest {

    private final JwtPropagationRequestInterceptor interceptor = new JwtPropagationRequestInterceptor();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static Jwt jwt() {
        return Jwt.withTokenValue("my-token-value")
                .header("alg", "HS256")
                .subject("123456")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }

    @Test
    void apply_withJwt_forwardsBearerToken() {
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt(), List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertThat(template.headers().get("Authorization")).containsExactly("Bearer my-token-value");
    }

    @Test
    void apply_anonymous_doesNotForward() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertThat(template.headers().get("Authorization")).isNull();
    }
}
