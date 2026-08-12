package com.yoursweakfoe.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@DisplayName("SecurityUtil — 安全工具类测试")
class SecurityUtilTest {

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setAuthentication(String principal, Object details, Collection<? extends GrantedAuthority> authorities) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, "credentials", authorities);
        if (details != null) {
            auth.setDetails(details);
        }
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ==================== getCurrentUserId ====================

    @Test
    void getCurrentUserId_withAuth_returnsId() {
        setAuthentication("user-123", null, List.of());
        assertThat(SecurityUtil.getCurrentUserId()).isEqualTo("user-123");
    }

    @Test
    void getCurrentUserId_noAuth_returnsNull() {
        assertThat(SecurityUtil.getCurrentUserId()).isNull();
    }

    // ==================== getUsername ====================

    @Test
    void getUsername_withIdentityDetails_returnsUsername() {
        setAuthentication("user-123", new IdentityDetails("john.doe", IdentitySource.EDGE), List.of());
        assertThat(SecurityUtil.getUsername()).isEqualTo("john.doe");
    }

    @Test
    void getUsername_withPlainStringDetails_returnsToString() {
        // 兼容非本框架建立的 Authentication（裸字符串 details）
        setAuthentication("user-123", "john.doe", List.of());
        assertThat(SecurityUtil.getUsername()).isEqualTo("john.doe");
    }

    @Test
    void getUsername_noAuth_returnsNull() {
        assertThat(SecurityUtil.getUsername()).isNull();
    }

    // ==================== getIdentitySource ====================

    @Test
    void getIdentitySource_edge_returnsEdge() {
        setAuthentication("u1", new IdentityDetails("john", IdentitySource.EDGE), List.of());
        assertThat(SecurityUtil.getIdentitySource()).isEqualTo(IdentitySource.EDGE);
    }

    @Test
    void getIdentitySource_foreignDetails_returnsNull() {
        setAuthentication("u1", "plain-details", List.of());
        assertThat(SecurityUtil.getIdentitySource()).isNull();
    }

    @Test
    void getIdentitySource_noAuth_returnsNull() {
        assertThat(SecurityUtil.getIdentitySource()).isNull();
    }

    // ==================== getRoles ====================

    @Test
    void getRoles_withAuthorities_stripsRolePrefix() {
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("READ_ONLY"));
        setAuthentication("user-123", null, authorities);
        assertThat(SecurityUtil.getRoles()).containsExactly("ADMIN", "USER", "READ_ONLY");
    }

    @Test
    void getRoles_noAuth_returnsEmptyList() {
        assertThat(SecurityUtil.getRoles()).isEmpty();
    }
}
