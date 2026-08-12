package com.yoursweakfoe.common.security;

/**
 * 安全传播常量 —— 网关透传 Header、gRPC Metadata Key、角色前缀的单一定义处。
 *
 * <p>入站（HTTP → 服务）走 Header，东西向（服务 → 服务 gRPC）走 Metadata，
 * 两组 Key 必须在此统一声明：任何一侧改名而另一侧遗漏，身份传播链会静默断裂。
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

    // region gRPC Metadata Key（东西向传播）

    /**
     * 身份 Metadata 统一前缀。
     *
     * <p>所有身份 Metadata Key 必须以其开头：多跳链路（A → B → C）中
     * 服务端 interceptor 解析、客户端 interceptor 重新写入，前缀是识别身份载荷的唯一约定。
     *
     * <p>gRPC Metadata Key 要求全小写（ASCII marshaller），下划线合法。
     */
    public static final String METADATA_PREFIX = "sec_";

    /** 用户ID Metadata Key（对应 {@link #HDR_USER_ID}） */
    public static final String METADATA_USER_ID = METADATA_PREFIX + "user_id";

    /** 用户名 Metadata Key（对应 {@link #HDR_USERNAME}） */
    public static final String METADATA_USERNAME = METADATA_PREFIX + "username";

    /** 角色列表 Metadata Key（对应 {@link #HDR_ROLES}，逗号分隔，不含 {@link #ROLE_PREFIX}） */
    public static final String METADATA_ROLES = METADATA_PREFIX + "roles";

    // endregion

    // region 角色约定

    /**
     * Spring Security 角色前缀。
     *
     * <p>传播载荷（Header / Attachment）中的角色**不含**此前缀，
     * 写入 {@code GrantedAuthority} 时补上，读出时剥离。
     */
    public static final String ROLE_PREFIX = "ROLE_";

    // endregion
}
