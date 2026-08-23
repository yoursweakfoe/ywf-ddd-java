package com.yoursweakfoe.common.cloud.feign.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import com.yoursweakfoe.common.cloud.feign.CommonCloudProperties;
import feign.RequestTemplate;
import feign.Target;
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

    private static final String DEFAULT_TOKEN = "my-token-value";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static Jwt jwt() {
        return Jwt.withTokenValue(DEFAULT_TOKEN)
                .header("alg", "HS256")
                .subject("123456")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }

    private static void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt(), List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private static CommonCloudProperties defaults() {
        return new CommonCloudProperties(true, "Authorization", "Bearer ", List.of());
    }

    private static RequestTemplate templateTo(String url) {
        return new RequestTemplate()
                .feignTarget(new Target.HardCodedTarget<>(Object.class, url));
    }

    @Test
    void apply_withJwt_forwardsBearerToken() {
        authenticate();
        JwtPropagationRequestInterceptor interceptor = new JwtPropagationRequestInterceptor(defaults());

        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertThat(template.headers().get("Authorization")).containsExactly("Bearer my-token-value");
    }

    @Test
    void apply_anonymous_doesNotForward() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        JwtPropagationRequestInterceptor interceptor = new JwtPropagationRequestInterceptor(defaults());

        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertThat(template.headers().get("Authorization")).isNull();
    }

    @Test
    void apply_disabled_doesNotForward() {
        authenticate();
        JwtPropagationRequestInterceptor interceptor = new JwtPropagationRequestInterceptor(
                new CommonCloudProperties(false, "Authorization", "Bearer ", List.of()));

        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertThat(template.headers().get("Authorization")).isNull();
    }

    @Test
    void apply_customHeaderAndPrefix() {
        authenticate();
        JwtPropagationRequestInterceptor interceptor = new JwtPropagationRequestInterceptor(
                new CommonCloudProperties(true, "X-Auth", "Token ", List.of()));

        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertThat(template.headers().get("X-Auth")).containsExactly("Token my-token-value");
        assertThat(template.headers().get("Authorization")).isNull();
    }

    @Test
    void apply_excludedHost_doesNotForward() {
        authenticate();
        JwtPropagationRequestInterceptor interceptor = new JwtPropagationRequestInterceptor(
                new CommonCloudProperties(true, "Authorization", "Bearer ",
                        List.of("api.external-payment.com")));

        RequestTemplate template = templateTo("http://api.external-payment.com/pay");
        interceptor.apply(template);

        assertThat(template.headers().get("Authorization")).isNull();
    }

    @Test
    void apply_nonExcludedHost_forwards() {
        authenticate();
        JwtPropagationRequestInterceptor interceptor = new JwtPropagationRequestInterceptor(
                new CommonCloudProperties(true, "Authorization", "Bearer ",
                        List.of("api.external-payment.com")));

        RequestTemplate template = templateTo("http://order-service.internal/orders");
        interceptor.apply(template);

        assertThat(template.headers().get("Authorization")).containsExactly("Bearer my-token-value");
    }
}