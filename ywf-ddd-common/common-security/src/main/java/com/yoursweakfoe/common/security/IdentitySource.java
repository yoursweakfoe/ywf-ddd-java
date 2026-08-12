package com.yoursweakfoe.common.security;

/**
 * 身份来源标记 —— 区分 SecurityContext 中的身份从哪个通道进入本服务。
 *
 * <p>身份可信源只在网关进入系统一次：{@link #EDGE} —— REST 入站，
 * 网关已验 JWT 并注入一手身份 Header（{@code SecurityWebFilter} 解析）。
 *
 * <p>东西向 HTTP 身份传播为未来设计，届时再扩展来源标记。
 *
 * <p>安全边界约定：内部服务端口不得直接暴露公网，否则身份 Header 可被伪造。
 */
public enum IdentitySource {

    /** 网关边界一手身份（REST 入站 Header 解析） */
    EDGE
}
