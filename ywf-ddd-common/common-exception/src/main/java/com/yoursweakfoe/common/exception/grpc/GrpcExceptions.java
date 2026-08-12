package com.yoursweakfoe.common.exception.grpc;

import com.yoursweakfoe.common.exception.BusinessException;
import io.grpc.StatusRuntimeException;

/**
 * gRPC 异常解包工具 —— Consumer 侧从 {@link StatusRuntimeException} 中提取
 * 由 {@code GrpcExceptionClientInterceptor} 还原的 {@link BusinessException}。
 */
public final class GrpcExceptions {

    private GrpcExceptions() {}

    /**
     * 提取业务异常。
     *
     * @param throwable gRPC 调用抛出的异常（通常为 StatusRuntimeException）
     * @return 还原的 BusinessException；非业务异常返回 null
     */
    public static BusinessException extractBusiness(Throwable throwable) {
        if (throwable instanceof BusinessException businessException) {
            return businessException;
        }
        if (throwable instanceof StatusRuntimeException
                && throwable.getCause() instanceof BusinessException businessException) {
            return businessException;
        }
        return null;
    }
}
