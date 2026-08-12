package com.yoursweakfoe.common.security;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全工具类 —— 从 SecurityContext 获取当前用户身份。
 *
 * <p>角色/权限判断请使用 {@code @PreAuthorize("hasRole('xxx')")} 方法级注解，
 * 无需在此提供 hasRole 等冗余 API。
 */
public final class SecurityUtil {

    private SecurityUtil() {}

    /**
     * 获取当前用户的 userId（字符串形式，兼容 UUID / Long 等任意 ID 类型）。
     *
     * @return userId 字符串，未登录时返回 null
     */
    public static String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        return auth.getPrincipal().toString();
    }

    /**
     * 获取当前用户的用户名（来自网关透传 Header 或上游 gRPC Metadata）。
     *
     * @return 用户名字符串，未登录或未透传时返回 null
     */
    public static String getUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getDetails() == null) {
            return null;
        }
        if (auth.getDetails() instanceof IdentityDetails details) {
            return details.username();
        }
        return auth.getDetails().toString();
    }

    /**
     * 获取当前身份的来源通道。
     *
     * @return {@link IdentitySource#EDGE}（REST 入站）/ {@link IdentitySource#PROPAGATED}（gRPC 入站），
     *         未登录或非本框架建立的身份返回 null
     */
    public static IdentitySource getIdentitySource() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        return auth.getDetails() instanceof IdentityDetails details ? details.source() : null;
    }

    /**
     * 获取当前用户的角色列表（已去除 {@link AuthConstants#ROLE_PREFIX} 前缀）。
     *
     * @return 角色名称列表，未登录时返回空列表
     */
    public static List<String> getRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return List.of();
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith(AuthConstants.ROLE_PREFIX)
                        ? a.substring(AuthConstants.ROLE_PREFIX.length())
                        : a)
                .toList();
    }
}
