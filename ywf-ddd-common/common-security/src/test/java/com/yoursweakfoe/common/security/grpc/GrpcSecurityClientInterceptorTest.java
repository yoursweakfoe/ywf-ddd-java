package com.yoursweakfoe.common.security.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yoursweakfoe.common.security.AuthConstants;
import com.yoursweakfoe.common.security.IdentityDetails;
import com.yoursweakfoe.common.security.IdentitySource;
import com.yoursweakfoe.common.security.SecurityContextSupport;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

@DisplayName("GrpcSecurityClientInterceptor — gRPC 出站身份传递")
@SuppressWarnings("unchecked")
class GrpcSecurityClientInterceptorTest {

    private final GrpcSecurityClientInterceptor interceptor = new GrpcSecurityClientInterceptor();

    private Channel channel;
    private ClientCall<Object, Object> call;
    private MethodDescriptor<Object, Object> method;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        channel = mock(Channel.class);
        call = mock(ClientCall.class);
        method = mock(MethodDescriptor.class);
        when(channel.newCall(any(), any())).thenReturn(call);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** 经出站拦截器启动调用，捕获实际写入的 Metadata。 */
    private Metadata startAndCaptureHeaders() {
        ClientCall<Object, Object> wrapped =
                interceptor.interceptCall(method, CallOptions.DEFAULT, channel);
        Metadata headers = new Metadata();
        wrapped.start(mock(ClientCall.Listener.class), headers);
        return headers;
    }

    @Test
    @DisplayName("有身份上下文：写入 sec_* Metadata（角色剥离 ROLE_ 前缀）")
    void interceptCall_withAuth_writesMetadata() {
        SecurityContextSupport.establish(
                SecurityContextSupport.buildAuthentication(
                        "user-1", "alice", "ROLE_ADMIN,USER", IdentitySource.EDGE));

        Metadata headers = startAndCaptureHeaders();

        assertThat(headers.get(SecurityMetadata.USER_ID)).isEqualTo("user-1");
        assertThat(headers.get(SecurityMetadata.USERNAME)).isEqualTo("alice");
        assertThat(headers.get(SecurityMetadata.ROLES)).isEqualTo("ADMIN,USER");
    }

    @Test
    @DisplayName("身份来源为 PROPAGATED 时同样向外传播（多跳链路）")
    void interceptCall_propagatedIdentity_alsoPropagates() {
        SecurityContextSupport.establish(
                SecurityContextSupport.buildAuthentication(
                        "rpc-user", "bob", "VIEWER", IdentitySource.PROPAGATED));

        Metadata headers = startAndCaptureHeaders();

        assertThat(headers.get(SecurityMetadata.USER_ID)).isEqualTo("rpc-user");
        assertThat(headers.get(SecurityMetadata.USERNAME)).isEqualTo("bob");
        assertThat(headers.get(SecurityMetadata.ROLES)).isEqualTo("VIEWER");
    }

    @Test
    @DisplayName("裸字符串 details 兼容：username 取 toString")
    void interceptCall_plainStringDetails_writesUsername() {
        var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "user-1", null, java.util.List.of());
        auth.setDetails("plain-name");
        SecurityContextHolder.getContext().setAuthentication(auth);

        Metadata headers = startAndCaptureHeaders();

        assertThat(headers.get(SecurityMetadata.USER_ID)).isEqualTo("user-1");
        assertThat(headers.get(SecurityMetadata.USERNAME)).isEqualTo("plain-name");
    }

    @Test
    @DisplayName("无身份上下文：不写入任何 sec_* Metadata")
    void interceptCall_noAuth_noMetadataWritten() {
        Metadata headers = startAndCaptureHeaders();

        assertThat(headers.get(SecurityMetadata.USER_ID)).isNull();
        assertThat(headers.get(SecurityMetadata.USERNAME)).isNull();
        assertThat(headers.get(SecurityMetadata.ROLES)).isNull();
    }

    @Test
    @DisplayName("有身份无角色：不写入 roles Key")
    void interceptCall_noRoles_noRolesKey() {
        SecurityContextSupport.establish(
                SecurityContextSupport.buildAuthentication("user-1", null, null, IdentitySource.EDGE));

        Metadata headers = startAndCaptureHeaders();

        assertThat(headers.get(SecurityMetadata.USER_ID)).isEqualTo("user-1");
        assertThat(headers.get(SecurityMetadata.ROLES)).isNull();
    }

    @Test
    @DisplayName("Metadata Key 与 AuthConstants 字符串一致（防两侧漂移）")
    void metadataKeys_matchAuthConstants() {
        assertThat(SecurityMetadata.USER_ID.name()).isEqualTo(AuthConstants.METADATA_USER_ID);
        assertThat(SecurityMetadata.USERNAME.name()).isEqualTo(AuthConstants.METADATA_USERNAME);
        assertThat(SecurityMetadata.ROLES.name()).isEqualTo(AuthConstants.METADATA_ROLES);
        assertThat(AuthConstants.METADATA_USER_ID).startsWith(AuthConstants.METADATA_PREFIX);
    }
}
