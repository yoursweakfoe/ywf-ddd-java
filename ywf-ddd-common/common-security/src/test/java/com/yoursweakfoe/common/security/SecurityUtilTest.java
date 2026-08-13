package com.yoursweakfoe.common.security;

import static org.assertj.core.api.Assertions.assertThat;

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

@DisplayName("SecurityUtil — 按名字读取任意 claim（不预定义字段）")
class SecurityUtilTest {

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static Jwt jwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("123456")
                .claim("uid", 123456L)                 // 数值 userId
                .claim("department", "研发部")          // 任意字段
                .claim("roles", List.of("ADMIN"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }

    private static void setJwt() {
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt(), List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @Test
    void getJwt_returnsRawJwt() {
        setJwt();
        assertThat(SecurityUtil.getJwt()).isNotNull();
        assertThat(SecurityUtil.getJwt().getSubject()).isEqualTo("123456");
    }

    @Test
    void getString_numericClaim_normalizedToString() {
        setJwt();
        assertThat(SecurityUtil.getString("uid")).isEqualTo("123456");
    }

    @Test
    void getString_anyField_readsByName() {
        setJwt();
        assertThat(SecurityUtil.getString("department")).isEqualTo("研发部");
    }

    @Test
    void getString_missingClaim_returnsNull() {
        setJwt();
        assertThat(SecurityUtil.getString("uname")).isNull();   // token 里没有用户名
    }

    @Test
    void getStringList_arrayClaim() {
        setJwt();
        assertThat(SecurityUtil.getStringList("roles")).containsExactly("ADMIN");
    }

    @Test
    void getStringList_commaSeparatedString() {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                Jwt.withTokenValue("t")
                        .header("alg", "HS256")
                        .claim("roles", "A,B")
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(300))
                        .build(),
                List.of()));
        assertThat(SecurityUtil.getStringList("roles")).containsExactly("A", "B");
    }

    @Test
    void getStringList_missingClaim_returnsEmpty() {
        setJwt();
        assertThat(SecurityUtil.getStringList("permissions")).isEmpty();
    }

    @Test
    void anonymous_returnsNull() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThat(SecurityUtil.getJwt()).isNull();
        assertThat(SecurityUtil.getString("uid")).isNull();
        assertThat(SecurityUtil.getStringList("roles")).isEmpty();
    }
}
