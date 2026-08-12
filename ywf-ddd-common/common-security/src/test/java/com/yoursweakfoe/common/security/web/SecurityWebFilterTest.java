package com.yoursweakfoe.common.security.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.yoursweakfoe.common.security.AuthConstants;
import com.yoursweakfoe.common.security.IdentitySource;
import com.yoursweakfoe.common.security.SecurityUtil;
import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@DisplayName("SecurityWebFilter — REST 入站身份解析（source=edge）")
class SecurityWebFilterTest {

    private final SecurityWebFilter filter = new SecurityWebFilter();
    private FilterChain chain;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        chain = mock(FilterChain.class);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("全量 Header：业务链内可读取身份，来源为 EDGE")
    void doFilterInternal_withAllHeaders_setsSecurityContext() throws Exception {
        request.addHeader(AuthConstants.HDR_USER_ID, "user-123");
        request.addHeader(AuthConstants.HDR_USERNAME, "john");
        request.addHeader(AuthConstants.HDR_ROLES, "ADMIN");

        AtomicReference<String> capturedUserId = new AtomicReference<>();
        AtomicReference<IdentitySource> capturedSource = new AtomicReference<>();
        doAnswer(inv -> {
            capturedUserId.set(SecurityUtil.getCurrentUserId());
            capturedSource.set(SecurityUtil.getIdentitySource());
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(request, response, chain);

        assertThat(capturedUserId.get()).isEqualTo("user-123");
        assertThat(capturedSource.get()).isEqualTo(IdentitySource.EDGE);
        assertThat(SecurityUtil.getUsername()).isNull(); // 链外已清理
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("缺失 userId Header：透传，不触碰 SecurityContext")
    void doFilterInternal_missingUserId_skipsSecurityContext() throws Exception {
        AtomicReference<Boolean> hasAuth = new AtomicReference<>(true);
        doAnswer(inv -> {
            hasAuth.set(SecurityContextHolder.getContext().getAuthentication() != null);
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(request, response, chain);

        assertThat(hasAuth.get()).isFalse();
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("空白 userId Header：透传，不触碰 SecurityContext")
    void doFilterInternal_blankUserId_skipsSecurityContext() throws Exception {
        request.addHeader(AuthConstants.HDR_USER_ID, "   ");

        AtomicReference<Boolean> hasAuth = new AtomicReference<>(true);
        doAnswer(inv -> {
            hasAuth.set(SecurityContextHolder.getContext().getAuthentication() != null);
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(request, response, chain);

        assertThat(hasAuth.get()).isFalse();
    }

    @Test
    @DisplayName("逗号分隔角色解析为 ROLE_ 权限")
    void doFilterInternal_withRoles_parsesCommaSeparated() throws Exception {
        request.addHeader(AuthConstants.HDR_USER_ID, "u1");
        request.addHeader(AuthConstants.HDR_ROLES, "ADMIN,USER");

        AtomicReference<String[]> capturedRoles = new AtomicReference<>();
        doAnswer(inv -> {
            capturedRoles.set(SecurityUtil.getRoles().toArray(String[]::new));
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(request, response, chain);

        assertThat(capturedRoles.get()).containsExactly("ADMIN", "USER");
    }

    @Test
    @DisplayName("已含 ROLE_ 前缀的角色不重复添加")
    void doFilterInternal_rolesWithPrefix_noDoublePrefix() throws Exception {
        request.addHeader(AuthConstants.HDR_USER_ID, "u1");
        request.addHeader(AuthConstants.HDR_ROLES, "ROLE_ADMIN,USER");

        AtomicReference<String[]> capturedRoles = new AtomicReference<>();
        doAnswer(inv -> {
            capturedRoles.set(SecurityUtil.getRoles().toArray(String[]::new));
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(request, response, chain);

        assertThat(capturedRoles.get()).containsExactly("ADMIN", "USER");
    }

    @Test
    @DisplayName("无角色 Header：权限列表为空")
    void doFilterInternal_noRoles_emptyAuthorities() throws Exception {
        request.addHeader(AuthConstants.HDR_USER_ID, "u1");

        AtomicReference<Integer> roleCount = new AtomicReference<>();
        doAnswer(inv -> {
            roleCount.set(SecurityUtil.getRoles().size());
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(request, response, chain);

        assertThat(roleCount.get()).isZero();
    }

    @Test
    @DisplayName("链正常结束后上下文已清理（线程池不泄漏）")
    void doFilterInternal_finallyClearsContext() throws Exception {
        request.addHeader(AuthConstants.HDR_USER_ID, "u1");
        request.addHeader(AuthConstants.HDR_USERNAME, "john");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("业务链抛异常：异常上抛且上下文仍被清理")
    void doFilterInternal_chainThrows_stillClearsContext() throws Exception {
        request.addHeader(AuthConstants.HDR_USER_ID, "u1");
        doThrow(new IllegalStateException("business failure")).when(chain).doFilter(any(), any());

        assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                .isInstanceOf(IllegalStateException.class);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
