package com.yoursweakfoe.common.security;

/**
 * Authentication 的 details 载荷 —— 用户名 + 身份来源。
 *
 * <p>复用 {@code UsernamePasswordAuthenticationToken#details} 槽位承载，
 * 避免引入自定义 Authentication 类型（序列化/框架兼容成本最低）。
 *
 * @param username 用户名（来自网关 Header，可为 null）
 * @param source   身份来源（edge，不为 null）
 */
public record IdentityDetails(String username, IdentitySource source) {
}
