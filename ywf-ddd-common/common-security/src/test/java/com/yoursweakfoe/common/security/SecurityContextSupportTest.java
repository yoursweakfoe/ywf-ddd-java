package com.yoursweakfoe.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@DisplayName("SecurityContextSupport — 身份上下文建立支撑测试")
class SecurityContextSupportTest {

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==================== buildAuthentication ====================

    @Test
    void buildAuthentication_setsPrincipalDetailsAndSource() {
        Authentication auth = SecurityContextSupport.buildAuthentication(
                "user-1", "alice", "ADMIN,USER", IdentitySource.EDGE);

        assertThat(auth.getPrincipal()).isEqualTo("user-1");
        assertThat(auth.getDetails()).isInstanceOf(IdentityDetails.class);
        IdentityDetails details = (IdentityDetails) auth.getDetails();
        assertThat(details.username()).isEqualTo("alice");
        assertThat(details.source()).isEqualTo(IdentitySource.EDGE);
        assertThat(auth.getAuthorities()).extracting(a -> a.getAuthority())
                .containsExactly("ROLE_ADMIN", "ROLE_USER");
        assertThat(auth.isAuthenticated()).isTrue();
    }

    @Test
    void buildAuthentication_nullUsernameAndRoles_tolerated() {
        Authentication auth = SecurityContextSupport.buildAuthentication(
                "user-1", null, null, IdentitySource.EDGE);

        IdentityDetails details = (IdentityDetails) auth.getDetails();
        assertThat(details.username()).isNull();
        assertThat(details.source()).isEqualTo(IdentitySource.EDGE);
        assertThat(auth.getAuthorities()).isEmpty();
    }

    // ==================== establish ====================

    @Test
    void establish_writesContextToCurrentThread() {
        SecurityContextSupport.establish("user-1", "alice", "ADMIN", IdentitySource.EDGE);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo("user-1");
        assertThat(SecurityUtil.getIdentitySource()).isEqualTo(IdentitySource.EDGE);
    }

    @Test
    void establish_withAuthentication_overridesResidualContext() {
        // 模拟线程池残留上下文：establish 必须整体替换而非合并
        SecurityContextSupport.establish("stale-user", "stale", "STALE_ROLE", IdentitySource.EDGE);

        Authentication fresh = SecurityContextSupport.buildAuthentication(
                "fresh-user", null, null, IdentitySource.EDGE);
        SecurityContextSupport.establish(fresh);

        assertThat(SecurityUtil.getCurrentUserId()).isEqualTo("fresh-user");
        assertThat(SecurityUtil.getIdentitySource()).isEqualTo(IdentitySource.EDGE);
        assertThat(SecurityUtil.getRoles()).isEmpty();
    }

    // ==================== clear ====================

    @Test
    void clear_removesAuthentication() {
        SecurityContextSupport.establish("user-1", "alice", null, IdentitySource.EDGE);
        SecurityContextSupport.clear();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // ==================== parseRoles ====================

    @Test
    void parseRoles_nullOrBlank_returnsEmpty() {
        assertThat(SecurityContextSupport.parseRoles(null)).isEmpty();
        assertThat(SecurityContextSupport.parseRoles("   ")).isEmpty();
    }

    @Test
    void parseRoles_commaSeparated_addsPrefix() {
        assertThat(SecurityContextSupport.parseRoles("ADMIN,USER"))
                .extracting(a -> a.getAuthority())
                .containsExactly("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void parseRoles_trimsWhitespaceAndSkipsEmpty() {
        assertThat(SecurityContextSupport.parseRoles(" ADMIN , ,USER,"))
                .extracting(a -> a.getAuthority())
                .containsExactly("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void parseRoles_existingPrefix_noDoublePrefix() {
        assertThat(SecurityContextSupport.parseRoles("ROLE_ADMIN,USER"))
                .extracting(a -> a.getAuthority())
                .containsExactly("ROLE_ADMIN", "ROLE_USER");
    }
}
