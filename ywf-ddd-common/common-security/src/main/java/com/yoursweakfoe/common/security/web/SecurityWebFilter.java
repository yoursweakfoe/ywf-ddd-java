package com.yoursweakfoe.common.security.web;

import com.yoursweakfoe.common.security.AuthConstants;
import com.yoursweakfoe.common.security.IdentitySource;
import com.yoursweakfoe.common.security.SecurityContextSupport;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * REST 入站安全过滤器 —— 解析 Higress 透传的身份 Header 并构建 SecurityContext（source=edge）。
 *
 * <p><b>职责边界（HTTP 入站通道）：</b>本 Filter 只负责 <b>HTTP 入站</b>身份解析，
 * 身份来源唯一为网关透传 Header（{@link AuthConstants#HDR_USER_ID} 等）。
 * gRPC 入站由 {@code GrpcSecurityServerInterceptor} 独立负责，两者互不依赖。
 *
 * <p><b>注册方式：</b>作为普通 Filter Bean 由 {@code SecurityAutoConfiguration} 注册，
 * Boot 自动将其加入 Servlet 过滤器链。
 *
 * <p><b>上下文生命周期：</b>业务方法（含其发起的下游 gRPC 调用）在
 * {@code filterChain.doFilter()} 内部同步执行，本 Filter 建立的 ThreadLocal
 * 上下文对整条链路可见；{@code finally} 中清理，防止线程归还线程池时身份泄漏。
 *
 * <p>本过滤器信任 Higress 网关已完成的 JWT 验签（JWKS），只负责解析透传 Header 构建身份上下文。
 *
 * <p><strong>安全前提条件（必须满足）：</strong>
 * <ul>
 *   <li>网关必须配置 jwt-auth 插件（Higress / Kong / APISIX 等），在转发前完成 JWT 验签并注入 X-User-* Header
 *   <li>服务端口不得直接暴露到公网（必须经网关转发，否则 Header 可被伪造）
 *   <li>若网关配置缺失或被绕过，本 Filter 不会拒绝请求（无 Header 时建立匿名上下文）——
 *       这是设计取舍：服务层不做重复验签，安全责任统一收口在网关
 * </ul>
 */
public class SecurityWebFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String userId = request.getHeader(AuthConstants.HDR_USER_ID);

        // 无 HTTP 身份（未经网关注入 / 匿名访问）：透传，不触碰 SecurityContext
        if (userId == null || userId.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        SecurityContextSupport.establish(
                userId,
                request.getHeader(AuthConstants.HDR_USERNAME),
                request.getHeader(AuthConstants.HDR_ROLES),
                IdentitySource.EDGE);
        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextSupport.clear();
        }
    }
}
