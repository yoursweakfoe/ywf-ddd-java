package com.yoursweakfoe.common.security.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yoursweakfoe.common.security.IdentitySource;
import com.yoursweakfoe.common.security.SecurityUtil;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

@DisplayName("GrpcSecurityServerInterceptor — gRPC 入站身份重建（source=propagated）")
@SuppressWarnings("unchecked")
class GrpcSecurityServerInterceptorTest {

    private final GrpcSecurityServerInterceptor interceptor = new GrpcSecurityServerInterceptor();

    private ServerCall<Object, Object> call;
    private ServerCallHandler<Object, Object> next;
    private ServerCall.Listener<Object> delegate;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        call = mock(ServerCall.class);
        next = mock(ServerCallHandler.class);
        delegate = mock(ServerCall.Listener.class);
        when(next.startCall(any(), any(Metadata.class))).thenReturn(delegate);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static Metadata identityHeaders(String userId, String username, String roles) {
        Metadata headers = new Metadata();
        if (userId != null) {
            headers.put(SecurityMetadata.USER_ID, userId);
        }
        if (username != null) {
            headers.put(SecurityMetadata.USERNAME, username);
        }
        if (roles != null) {
            headers.put(SecurityMetadata.ROLES, roles);
        }
        return headers;
    }

    @Test
    @DisplayName("携带身份 Metadata：业务回调内可读取身份，来源为 PROPAGATED")
    void interceptCall_withMetadata_establishesContextDuringCallbacks() {
        AtomicReference<String> capturedUserId = new AtomicReference<>();
        AtomicReference<String> capturedUsername = new AtomicReference<>();
        AtomicReference<IdentitySource> capturedSource = new AtomicReference<>();
        AtomicReference<String[]> capturedRoles = new AtomicReference<>();
        doAnswer(inv -> {
            capturedUserId.set(SecurityUtil.getCurrentUserId());
            capturedUsername.set(SecurityUtil.getUsername());
            capturedSource.set(SecurityUtil.getIdentitySource());
            capturedRoles.set(SecurityUtil.getRoles().toArray(String[]::new));
            return null;
        }).when(delegate).onHalfClose();

        ServerCall.Listener<Object> listener = interceptor.interceptCall(
                call, identityHeaders("rpc-user", "bob", "VIEWER,ADMIN"), next);

        listener.onHalfClose();

        assertThat(capturedUserId.get()).isEqualTo("rpc-user");
        assertThat(capturedUsername.get()).isEqualTo("bob");
        assertThat(capturedSource.get()).isEqualTo(IdentitySource.PROPAGATED);
        assertThat(capturedRoles.get()).containsExactly("VIEWER", "ADMIN");
        // 回调结束后上下文已清理
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("全部五个回调入口均建立/清理上下文")
    void interceptCall_allCallbacks_wrapped() {
        AtomicReference<Integer> establishedCount = new AtomicReference<>(0);
        doAnswer(inv -> {
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                establishedCount.updateAndGet(c -> c + 1);
            }
            return null;
        }).when(delegate).onMessage(any());
        doAnswer(inv -> {
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                establishedCount.updateAndGet(c -> c + 1);
            }
            return null;
        }).when(delegate).onHalfClose();
        doAnswer(inv -> {
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                establishedCount.updateAndGet(c -> c + 1);
            }
            return null;
        }).when(delegate).onCancel();
        doAnswer(inv -> {
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                establishedCount.updateAndGet(c -> c + 1);
            }
            return null;
        }).when(delegate).onComplete();
        doAnswer(inv -> {
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                establishedCount.updateAndGet(c -> c + 1);
            }
            return null;
        }).when(delegate).onReady();

        ServerCall.Listener<Object> listener = interceptor.interceptCall(
                call, identityHeaders("u1", null, null), next);

        listener.onMessage(new Object());
        listener.onHalfClose();
        listener.onCancel();
        listener.onComplete();
        listener.onReady();

        assertThat(establishedCount.get()).isEqualTo(5);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("无身份 Metadata：直接透传，返回原始监听器且不触碰上下文")
    void interceptCall_noMetadata_passThrough() {
        ServerCall.Listener<Object> listener =
                interceptor.interceptCall(call, new Metadata(), next);

        assertThat(listener).isSameAs(delegate);
        listener.onHalfClose();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("空白 userId Metadata：直接透传")
    void interceptCall_blankUserId_passThrough() {
        ServerCall.Listener<Object> listener =
                interceptor.interceptCall(call, identityHeaders("   ", null, null), next);

        assertThat(listener).isSameAs(delegate);
    }

    @Test
    @DisplayName("业务回调抛异常：异常上抛且上下文仍被清理")
    void interceptCall_callbackThrows_stillClearsContext() {
        doThrow(new IllegalStateException("business failure")).when(delegate).onHalfClose();

        ServerCall.Listener<Object> listener = interceptor.interceptCall(
                call, identityHeaders("u1", null, null), next);

        assertThatThrownBy(listener::onHalfClose)
                .isInstanceOf(IllegalStateException.class);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
