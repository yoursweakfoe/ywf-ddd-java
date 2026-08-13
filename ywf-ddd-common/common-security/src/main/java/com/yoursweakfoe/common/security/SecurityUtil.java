package com.yoursweakfoe.common.security;

import java.util.Arrays;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * 安全工具类 —— 按名字读取当前 JWT 的 claim。
 *
 * <p>公司 JWT 字段命名 / 数量无规范，故不预定义字段，只按名字取（缺失返回 null / 空）。
 * 仅允许在 Application / Adapter 层调用；Domain 层禁止。
 */
public final class SecurityUtil {

    private SecurityUtil() {}

    /** 原始已验签 JWT（claims 全量）；匿名时返回 null。 */
    public static Jwt getJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        if (auth.getPrincipal() instanceof Jwt jwt) {
            return jwt;
        }
        if (auth.getCredentials() instanceof Jwt jwt) {
            return jwt;
        }
        return null;
    }

    /** 读取任意 claim 的原值；匿名或缺失时返回 null。 */
    public static Object getClaim(String name) {
        Jwt jwt = getJwt();
        return jwt == null ? null : jwt.getClaim(name);
    }

    /** 读取任意 claim 的字符串形式（数值自动归一）；匿名或缺失时返回 null。 */
    public static String getString(String name) {
        Object value = getClaim(name);
        return value == null ? null : String.valueOf(value);
    }

    /** 读取任意 claim 的字符串列表（数组或逗号串）；匿名或缺失时返回空列表。 */
    public static List<String> getStringList(String name) {
        Object value = getClaim(name);
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        if (value instanceof String s && !s.isBlank()) {
            return Arrays.stream(s.split(","))
                    .map(String::trim)
                    .filter(x -> !x.isEmpty())
                    .toList();
        }
        return List.of();
    }
}
