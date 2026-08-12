package com.yoursweakfoe.common.security.grpc;

import com.yoursweakfoe.common.security.AuthConstants;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * gRPC 出站安全拦截器 —— 将当前线程 SecurityContext 中的用户身份写入 Metadata，
 * 随 gRPC 调用传递到下游服务。
 *
 * <p><b>传播字段：</b>
 * <ul>
 *   <li>{@code sec_user_id} — 用户 ID（principal）
 *   <li>{@code sec_username} — 用户名（details）
 *   <li>{@code sec_roles} — 角色列表（authorities，逗号分隔，不含 {@code ROLE_} 前缀）
 * </ul>
 *
 * <p>身份无论来源（REST edge / gRPC propagated）均一视同仁地向外传播——
 * 多跳链路中每一跳的出站拦截器都会从本地 SecurityContext 重新写入 Metadata，
 * 逐跳自动完成透传，无需额外的透传选择器机制。
 *
 * <p>若当前线程无 SecurityContext 或 Authentication 为空，则不写入任何 Metadata，直接透传调用。
 *
 * <p>注册：{@code SecurityAutoConfiguration} 以 {@code @GlobalClientInterceptor} Bean 装配。
 */
public class GrpcSecurityClientInterceptor implements ClientInterceptor {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() != null) {
                    headers.put(SecurityMetadata.USER_ID, auth.getPrincipal().toString());

                    String username = extractUsername(auth);
                    if (username != null) {
                        headers.put(SecurityMetadata.USERNAME, username);
                    }

                    String roles = auth.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .map(GrpcSecurityClientInterceptor::stripRolePrefix)
                            .collect(Collectors.joining(","));
                    if (!roles.isEmpty()) {
                        headers.put(SecurityMetadata.ROLES, roles);
                    }
                }
                super.start(responseListener, headers);
            }
        };
    }

    /** 读取用户名（兼容 IdentityDetails 与裸字符串 details）。 */
    private static String extractUsername(Authentication auth) {
        if (auth.getDetails() == null) {
            return null;
        }
        if (auth.getDetails() instanceof com.yoursweakfoe.common.security.IdentityDetails details) {
            return details.username();
        }
        return auth.getDetails().toString();
    }

    /** 剥除 {@code ROLE_} 前缀（传播载荷约定不含前缀）。 */
    private static String stripRolePrefix(String authority) {
        return authority.startsWith(AuthConstants.ROLE_PREFIX)
                ? authority.substring(AuthConstants.ROLE_PREFIX.length())
                : authority;
    }
}
