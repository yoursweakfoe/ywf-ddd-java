package com.yoursweakfoe.common.security;

/**
 * 安全传播常量 —— 网关透传 Header、角色前缀的单一定义处。
 *
 * <p>入站（HTTP → 服务）走 Header，Key 必须在此统一声明。
 *
 * <p>Higress 配置示例：
 * <pre>
 * claims_to_headers:
 *   - claim: sub
 *     header: X-User-Id
 *   - claim: username
 *     header: X-Username
 *   - claim: roles
 *     header: X-Roles
 * </pre>
 */
public final class AuthConstants {

    private AuthConstants() {}

    // region 网关透传 Header（REST 入站）

    /** 用户ID（JWT sub claim） */
    public static final String HDR_USER_ID = "X-User-Id";

    /** 用户名（JWT username claim） */
    public static final String HDR_USERNAME = "X-Username";

    /** 角色列表（JWT roles claim，逗号分隔） */
    public static final String HDR_ROLES = "X-Roles";

    // endregion

    // region 角色约定

    /**
     * Spring Security 角色前缀。
     *
     * <p>传播载荷（Header）中的角色**不含**此前缀，
     * 写入 {@code GrantedAuthority} 时补上，读出时剥离。
     */
    public static final String ROLE_PREFIX = "ROLE_";

    // endregion
}
