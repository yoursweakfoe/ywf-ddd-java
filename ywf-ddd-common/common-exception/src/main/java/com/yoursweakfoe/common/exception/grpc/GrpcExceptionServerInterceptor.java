package com.yoursweakfoe.common.exception.grpc;

import com.yoursweakfoe.common.exception.BusinessException;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * gRPC 通道全局异常处理拦截器（服务端）。
 *
 * <p>与 {@code GlobalRestExceptionHandler}（REST 通道）互补，负责拦截 gRPC 服务端
 * 业务回调中的异常，将其映射为 {@link StatusRuntimeException} 语义的 Status + Trailers，
 * 避免原始 Java 异常堆栈透传（泄漏内部类名、行号等实现细节）。
 *
 * <p><b>异常映射规则（与 REST 通道保持语义一致）：</b>
 * <ul>
 *   <li>{@link BusinessException} → FAILED_PRECONDITION，messageKey 放 status description，
 *       messageKey 与 params（JSON）写入 Trailers（{@link GrpcExceptionMetadata}），
 *       供客户端 interceptor 还原 BusinessException</li>
 *   <li>{@link IllegalStateException} → FAILED_PRECONDITION（状态冲突）</li>
 *   <li>{@link IllegalArgumentException} → INVALID_ARGUMENT（参数校验）</li>
 *   <li>其他 {@link RuntimeException} → INTERNAL（原始信息仅写日志）</li>
 * </ul>
 *
 * <p><b>线程模型：</b>与身份拦截器同理，业务代码运行于 gRPC 回调线程，
 * 异常捕获通过监听器代理在每个回调周围完成。
 *
 * <p>注册：{@code ExceptionAutoConfiguration} 以 {@code @GlobalServerInterceptor} Bean 装配。
 */
public class GrpcExceptionServerInterceptor implements ServerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(GrpcExceptionServerInterceptor.class);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        return new ExceptionHandlingListener<>(next.startCall(call, headers), call);
    }

    /** 监听器代理 —— 捕获业务回调异常，映射为 Status 并关闭调用。 */
    static final class ExceptionHandlingListener<ReqT>
            extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {

        private final ServerCall<ReqT, ?> call;

        ExceptionHandlingListener(ServerCall.Listener<ReqT> delegate, ServerCall<ReqT, ?> call) {
            super(delegate);
            this.call = call;
        }

        @Override
        public void onMessage(ReqT message) {
            guard(() -> super.onMessage(message));
        }

        @Override
        public void onHalfClose() {
            guard(super::onHalfClose);
        }

        @Override
        public void onCancel() {
            guard(super::onCancel);
        }

        @Override
        public void onComplete() {
            guard(super::onComplete);
        }

        @Override
        public void onReady() {
            guard(super::onReady);
        }

        private void guard(Runnable action) {
            try {
                action.run();
            } catch (BusinessException e) {
                log.warn("Business error in gRPC [{}]: {} | params: {}",
                        call.getMethodDescriptor().getFullMethodName(), e.getMessage(), e.getParams());
                closeWithBusiness(e);
            } catch (IllegalStateException e) {
                log.warn("Illegal state in gRPC [{}]: {}",
                        call.getMethodDescriptor().getFullMethodName(), e.getMessage());
                close(Status.FAILED_PRECONDITION.withDescription(e.getMessage()), new Metadata());
            } catch (IllegalArgumentException e) {
                log.warn("Bad request in gRPC [{}]: {}",
                        call.getMethodDescriptor().getFullMethodName(), e.getMessage());
                close(Status.INVALID_ARGUMENT.withDescription(e.getMessage()), new Metadata());
            } catch (RuntimeException e) {
                log.error("Unhandled exception in gRPC [{}]",
                        call.getMethodDescriptor().getFullMethodName(), e);
                close(Status.INTERNAL.withDescription("Internal error"), new Metadata());
            }
        }

        /** BusinessException → FAILED_PRECONDITION + Trailers（messageKey / params JSON）。 */
        private void closeWithBusiness(BusinessException e) {
            Metadata trailers = new Metadata();
            trailers.put(GrpcExceptionMetadata.MESSAGE_KEY, e.getMessage());
            if (!e.getParams().isEmpty()) {
                trailers.put(GrpcExceptionMetadata.PARAMS, GrpcExceptionParamsCodec.encode(e.getParams()));
            }
            close(Status.FAILED_PRECONDITION.withDescription(e.getMessage()), trailers);
        }

        /**
         * 关闭调用。回调可能在调用已关闭后抛异常（如流式场景的竞态），
         * 此时重复 close 会抛 IllegalStateException——降级为调试日志，不掩盖原始异常语义。
         */
        private void close(Status status, Metadata trailers) {
            try {
                call.close(status, trailers);
            } catch (IllegalStateException alreadyClosed) {
                log.debug("gRPC call already closed when mapping exception", alreadyClosed);
            }
        }
    }
}
