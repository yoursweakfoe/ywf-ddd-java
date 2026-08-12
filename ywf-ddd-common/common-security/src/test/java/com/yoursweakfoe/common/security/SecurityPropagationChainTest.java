package com.yoursweakfoe.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yoursweakfoe.common.security.grpc.GrpcSecurityClientInterceptor;
import com.yoursweakfoe.common.security.grpc.GrpcSecurityServerInterceptor;
import com.yoursweakfoe.common.security.grpc.SecurityMetadata;
import com.yoursweakfoe.common.security.web.SecurityWebFilter;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 身份传播链路契约测试 —— 按真实通道组合串联组件，验证 REST 与 gRPC
 * 两条入站路径的身份可见性、来源标记，以及向下游 gRPC 的传播。
 *
 * <h3>为什么需要本测试</h3>
 * 单组件测试无法暴露"通道衔接"层面的缺陷。本测试守护两条核心契约：
 * <ol>
 *   <li>REST 边界建立的一手身份（edge）经业务代码发起的 gRPC 调用传递到下游
 *   <li>gRPC 入站的传递身份（propagated）在多跳链路（A → B → C）中继续传播
 * </ol>
 */
@DisplayName("身份传播链路契约 — REST 与 gRPC 双路径")
@SuppressWarnings("unchecked")
class SecurityPropagationChainTest {

    private final SecurityWebFilter webFilter = new SecurityWebFilter();
    private final GrpcSecurityServerInterceptor serverInterceptor = new GrpcSecurityServerInterceptor();
    private final GrpcSecurityClientInterceptor clientInterceptor = new GrpcSecurityClientInterceptor();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // region 测试辅助

    /** 模拟业务代码在 REST 链内发起一次下游 gRPC 调用，返回出站写入的 Metadata。 */
    private Metadata downstreamGrpcCall() {
        Channel channel = mock(Channel.class);
        ClientCall<Object, Object> call = mock(ClientCall.class);
        when(channel.newCall(any(), any())).thenReturn(call);
        Metadata headers = new Metadata();
        ClientCall<Object, Object> wrapped = clientInterceptor.interceptCall(
                mock(MethodDescriptor.class), CallOptions.DEFAULT, channel);
        wrapped.start(mock(ClientCall.Listener.class), headers);
        return headers;
    }

    /** 构造携带身份的入站 gRPC Metadata。 */
    private static Metadata identityHeaders(String userId, String username, String roles) {
        Metadata headers = new Metadata();
        headers.put(SecurityMetadata.USER_ID, userId);
        if (username != null) {
            headers.put(SecurityMetadata.USERNAME, username);
        }
        if (roles != null) {
            headers.put(SecurityMetadata.ROLES, roles);
        }
        return headers;
    }

    // endregion

    @Nested
    @DisplayName("REST 入站路径（网关 → HTTP Header）")
    class RestInbound {

        @Test
        @DisplayName("业务方法可读取 HTTP 身份，来源标记为 EDGE")
        void restInbound_identityVisibleToBusiness() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(AuthConstants.HDR_USER_ID, "rest-user");
            request.addHeader(AuthConstants.HDR_USERNAME, "alice");
            request.addHeader(AuthConstants.HDR_ROLES, "ADMIN,USER");

            AtomicReference<String> businessUserId = new AtomicReference<>();
            AtomicReference<IdentitySource> businessSource = new AtomicReference<>();
            FilterChain chain = mock(FilterChain.class);
            doAnswer(inv -> {
                businessUserId.set(SecurityUtil.getCurrentUserId());
                businessSource.set(SecurityUtil.getIdentitySource());
                return null;
            }).when(chain).doFilter(any(), any());

            webFilter.doFilter(request, new MockHttpServletResponse(), chain);

            assertThat(businessUserId.get()).isEqualTo("rest-user");
            assertThat(businessSource.get()).isEqualTo(IdentitySource.EDGE);
        }

        @Test
        @DisplayName("REST 一手身份可传播到下游 gRPC（edge → Metadata）")
        void restInbound_propagatesToDownstreamGrpc() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(AuthConstants.HDR_USER_ID, "rest-user");
            request.addHeader(AuthConstants.HDR_USERNAME, "alice");
            request.addHeader(AuthConstants.HDR_ROLES, "ROLE_ADMIN");

            AtomicReference<Metadata> captured = new AtomicReference<>();
            FilterChain chain = mock(FilterChain.class);
            doAnswer(inv -> {
                captured.set(downstreamGrpcCall());
                return null;
            }).when(chain).doFilter(any(), any());

            webFilter.doFilter(request, new MockHttpServletResponse(), chain);

            assertThat(captured.get().get(SecurityMetadata.USER_ID)).isEqualTo("rest-user");
            assertThat(captured.get().get(SecurityMetadata.USERNAME)).isEqualTo("alice");
            // 传播载荷不含 ROLE_ 前缀
            assertThat(captured.get().get(SecurityMetadata.ROLES)).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("链路结束后上下文已清理（线程池不泄漏）")
        void restInbound_contextClearedAfterChain() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(AuthConstants.HDR_USER_ID, "rest-user");

            webFilter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Nested
    @DisplayName("gRPC 入站路径（上游服务 → Metadata）")
    class GrpcInbound {

        @Test
        @DisplayName("业务回调可读取 RPC 身份，来源标记为 PROPAGATED")
        void grpcInbound_identityVisibleToBusiness() {
            ServerCall<Object, Object> call = mock(ServerCall.class);
            ServerCallHandler<Object, Object> next = mock(ServerCallHandler.class);
            ServerCall.Listener<Object> delegate = mock(ServerCall.Listener.class);
            when(next.startCall(any(), any(Metadata.class))).thenReturn(delegate);

            AtomicReference<String> businessUserId = new AtomicReference<>();
            AtomicReference<IdentitySource> businessSource = new AtomicReference<>();
            doAnswer(inv -> {
                businessUserId.set(SecurityUtil.getCurrentUserId());
                businessSource.set(SecurityUtil.getIdentitySource());
                return null;
            }).when(delegate).onHalfClose();

            ServerCall.Listener<Object> listener = serverInterceptor.interceptCall(
                    call, identityHeaders("rpc-user", "bob", "VIEWER"), next);
            listener.onHalfClose();

            assertThat(businessUserId.get()).isEqualTo("rpc-user");
            assertThat(businessSource.get()).isEqualTo(IdentitySource.PROPAGATED);
        }

        @Test
        @DisplayName("多跳 A→B→C：RPC 入站身份可继续传播到下游")
        void grpcInbound_propagatesToNextHop() {
            ServerCall<Object, Object> call = mock(ServerCall.class);
            ServerCallHandler<Object, Object> next = mock(ServerCallHandler.class);
            ServerCall.Listener<Object> delegate = mock(ServerCall.Listener.class);
            when(next.startCall(any(), any(Metadata.class))).thenReturn(delegate);

            AtomicReference<Metadata> captured = new AtomicReference<>();
            doAnswer(inv -> {
                captured.set(downstreamGrpcCall());
                return null;
            }).when(delegate).onHalfClose();

            ServerCall.Listener<Object> listener = serverInterceptor.interceptCall(
                    call, identityHeaders("rpc-user", "bob", "VIEWER"), next);
            listener.onHalfClose();

            assertThat(captured.get().get(SecurityMetadata.USER_ID)).isEqualTo("rpc-user");
            assertThat(captured.get().get(SecurityMetadata.USERNAME)).isEqualTo("bob");
            assertThat(captured.get().get(SecurityMetadata.ROLES)).isEqualTo("VIEWER");
        }
    }

    @Nested
    @DisplayName("匿名路径")
    class Anonymous {

        @Test
        @DisplayName("REST 无身份时，业务侧读到空身份且下游无 Metadata 写入")
        void anonymous_noIdentityAnywhere() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();

            AtomicReference<String> businessUserId = new AtomicReference<>("sentinel");
            AtomicReference<Metadata> captured = new AtomicReference<>();
            FilterChain chain = mock(FilterChain.class);
            doAnswer(inv -> {
                businessUserId.set(SecurityUtil.getCurrentUserId());
                captured.set(downstreamGrpcCall());
                return null;
            }).when(chain).doFilter(any(), any());

            webFilter.doFilter(request, new MockHttpServletResponse(), chain);

            assertThat(businessUserId.get()).isNull();
            assertThat(captured.get().get(SecurityMetadata.USER_ID)).isNull();
            assertThat(captured.get().get(SecurityMetadata.USERNAME)).isNull();
            assertThat(captured.get().get(SecurityMetadata.ROLES)).isNull();
        }
    }
}
