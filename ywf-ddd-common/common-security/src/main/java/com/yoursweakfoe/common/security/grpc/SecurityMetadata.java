package com.yoursweakfoe.common.security.grpc;

import com.yoursweakfoe.common.security.AuthConstants;
import io.grpc.Metadata;

/**
 * 身份传播 Metadata Key 定义 —— gRPC 东西向通道的 {@code sec_*} 键。
 *
 * <p>Key 字符串统一定义于 {@link AuthConstants}（与 REST Header 集中管理），
 * 此处仅声明对应的 {@link Metadata.Key} 实例（ASCII marshaller，值必须 ASCII）。
 */
public final class SecurityMetadata {

    private SecurityMetadata() {}

    /** 用户ID（对应 {@link AuthConstants#METADATA_USER_ID}） */
    public static final Metadata.Key<String> USER_ID =
            Metadata.Key.of(AuthConstants.METADATA_USER_ID, Metadata.ASCII_STRING_MARSHALLER);

    /** 用户名（对应 {@link AuthConstants#METADATA_USERNAME}） */
    public static final Metadata.Key<String> USERNAME =
            Metadata.Key.of(AuthConstants.METADATA_USERNAME, Metadata.ASCII_STRING_MARSHALLER);

    /** 角色列表，逗号分隔不含 ROLE_ 前缀（对应 {@link AuthConstants#METADATA_ROLES}） */
    public static final Metadata.Key<String> ROLES =
            Metadata.Key.of(AuthConstants.METADATA_ROLES, Metadata.ASCII_STRING_MARSHALLER);
}
