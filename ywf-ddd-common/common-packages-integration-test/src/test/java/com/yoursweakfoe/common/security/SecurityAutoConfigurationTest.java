package com.yoursweakfoe.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

@DisplayName("SecurityAutoConfiguration — 角色 claim 名可配置")
class SecurityAutoConfigurationTest {

    private static Jwt jwt(String claimName, List<String> roles) {
        return Jwt.withTokenValue("t")
                .header("alg", "HS256")
                .subject("u1")
                .claim(claimName, roles)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }

    @Test
    void rolesClaim_defaultsToRoles() {
        JwtAuthenticationConverter converter =
                new SecurityAutoConfiguration().jwtAuthenticationConverter(
                        new SecurityProperties(true, "roles", "ROLE_"));

        Authentication auth = converter.convert(jwt("roles", List.of("ADMIN")));

        // Spring Security 7 会额外塞 FACTOR_BEARER（MFA 单因子标记），只断言 ROLE_* 在即可
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_ADMIN");
    }

    @Test
    void rolesClaim_customizable() {
        SecurityProperties properties = new SecurityProperties(true, "permissions", "ROLE_");

        JwtAuthenticationConverter converter =
                new SecurityAutoConfiguration().jwtAuthenticationConverter(properties);

        Authentication auth = converter.convert(jwt("permissions", List.of("ADMIN", "USER")));

        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void authorityPrefix_customizable() {
        SecurityProperties properties = new SecurityProperties(true, "roles", "SCOPE_");

        JwtAuthenticationConverter converter =
                new SecurityAutoConfiguration().jwtAuthenticationConverter(properties);

        Authentication auth = converter.convert(jwt("roles", List.of("ADMIN")));

        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .contains("SCOPE_ADMIN");
    }
}
