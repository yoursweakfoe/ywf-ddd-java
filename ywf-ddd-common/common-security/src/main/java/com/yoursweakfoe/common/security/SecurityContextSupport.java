package com.yoursweakfoe.common.security;

import java.util.Arrays;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 身份上下文建立支撑 —— REST 入站（{@code SecurityWebFilter}）与
 * gRPC 入站（{@code GrpcSecurityServerInterceptor}）共用的构建 + 写入逻辑。
 *
 * <p>两个入口的身份来源不同（Header / Metadata），但"解析角色 → 构建 Authentication →
 * 写入 ThreadLocal"完全一致，集中于此避免两侧实现漂移。身份来源以
 * {@link IdentityDetails} 标记在 Authentication 的 details 槽位。
 *
 * <p><b>清理责任</b>：本类只负责建立，调用方**必须**在 {@code finally} 中调用
 * {@link #clear()}，否则线程归还线程池时身份泄漏给后续请求。
 */
public final class SecurityContextSupport {

    private SecurityContextSupport() {}

    /**
     * 构建 Authentication（不写入上下文）。
     *
     * @param userId   用户 ID（principal，调用方需先判空）
     * @param username 用户名（可为 null）
     * @param roles    逗号分隔角色（不含 {@code ROLE_} 前缀，可为 null）
     * @param source   身份来源（edge / propagated）
     */
    public static Authentication buildAuthentication(
            String userId, String username, String roles, IdentitySource source) {
        var authentication =
                new UsernamePasswordAuthenticationToken(userId, null, parseRoles(roles));
        authentication.setDetails(new IdentityDetails(username, source));
        return authentication;
    }

    /**
     * 建立当前线程的 SecurityContext。
     *
     * <p>使用 {@code createEmptyContext()} 而非改写已有 Context：避免污染线程池中
     * 上一次请求可能残留的 Context 实例（Spring Security 推荐做法）。
     *
     * @param userId   用户 ID（principal，调用方需先判空）
     * @param username 用户名（可为 null）
     * @param roles    逗号分隔角色（不含 {@code ROLE_} 前缀，可为 null）
     * @param source   身份来源（edge / propagated）
     */
    public static void establish(String userId, String username, String roles, IdentitySource source) {
        establish(buildAuthentication(userId, username, roles, source));
    }

    /** 以已构建的 Authentication 建立当前线程的 SecurityContext。 */
    public static void establish(Authentication authentication) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    /** 清理当前线程 SecurityContext（必须在 finally 中调用，防线程池泄漏）。 */
    public static void clear() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 解析逗号分隔的角色字符串，自动补充 {@link AuthConstants#ROLE_PREFIX}
     * 以兼容 Spring Security 约定。
     *
     * @param roles 逗号分隔的角色名称（通常不含前缀；已含前缀者不重复添加）
     * @return GrantedAuthority 列表，入参为空时返回空列表
     */
    public static List<GrantedAuthority> parseRoles(String roles) {
        if (roles == null || roles.isBlank()) {
            return List.of();
        }
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(r -> !r.isEmpty())
                .map(r -> r.startsWith(AuthConstants.ROLE_PREFIX)
                        ? r
                        : AuthConstants.ROLE_PREFIX + r)
                .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                .toList();
    }
}
