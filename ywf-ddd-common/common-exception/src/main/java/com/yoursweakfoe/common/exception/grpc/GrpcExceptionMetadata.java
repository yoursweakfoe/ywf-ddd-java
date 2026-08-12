package com.yoursweakfoe.common.exception.grpc;

import io.grpc.Metadata;

/**
 * gRPC 异常通道 Trailer Key 定义 —— BusinessException 跨通道还原的载荷键。
 *
 * <p>服务端 interceptor 将 BusinessException 映射为 StatusRuntimeException 时，
 * messageKey 写入 {@link #MESSAGE_KEY}（同时冗余在 status description），
 * params 以 JSON 写入 {@link #PARAMS}（二进制键，{@code -bin} 后缀自动 base64）；
 * 客户端 interceptor 据此还原 BusinessException。
 */
public final class GrpcExceptionMetadata {

    private GrpcExceptionMetadata() {}

    /** 业务异常 i18n messageKey（ASCII） */
    public static final Metadata.Key<String> MESSAGE_KEY =
            Metadata.Key.of("biz-message-key", Metadata.ASCII_STRING_MARSHALLER);

    /** 业务异常 params 的 JSON 字节（二进制键） */
    public static final Metadata.Key<byte[]> PARAMS =
            Metadata.Key.of("biz-params-bin", Metadata.BINARY_BYTE_MARSHALLER);
}
