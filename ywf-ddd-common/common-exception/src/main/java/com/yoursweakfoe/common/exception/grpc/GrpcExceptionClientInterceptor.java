package com.yoursweakfoe.common.exception.grpc;

import com.yoursweakfoe.common.exception.BusinessException;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import java.util.Map;

/**
 * gRPC 通道客户端异常还原拦截器（Consumer 侧）。
 *
 * <p>服务端 {@code GrpcExceptionServerInterceptor} 将 {@link BusinessException}
 * 映射为 Status + Trailers（messageKey / params JSON）；本拦截器在客户端
 * {@code onClose} 时检测 Trailers 中的业务异常标记，将其还原为
 * {@link BusinessException} 并挂载到 Status 的 cause 上。
 *
 * <p><b>Consumer 捕获方式：</b>gRPC stub 抛出的异常类型
 * 固定为 {@link io.grpc.StatusRuntimeException}，还原的 BusinessException 位于其
 * {@code cause}。Consumer 侧写法：
 * <pre>{@code
 * try {
 *     ProductInfo reply = stub.getProduct(request);
 * } catch (StatusRuntimeException e) {
 *     BusinessException biz = GrpcExceptions.extractBusiness(e);
 *     if (biz != null) {
 *         // biz.getMessage() → i18n 位点；biz.getParams() → 占位符参数
 *     } else {
 *         throw e; // 系统级错误原样上抛
 *     }
 * }
 * }</pre>
 *
 * <p>注册：{@code ExceptionAutoConfiguration} 以 {@code @GlobalClientInterceptor} Bean 装配。
 */
public class GrpcExceptionClientInterceptor implements ClientInterceptor {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                super.start(new ExceptionRestoringListener<>(responseListener), headers);
            }
        };
    }

    /** 监听器代理 —— onClose 时依据 Trailers 还原 BusinessException 至 Status cause。 */
    static final class ExceptionRestoringListener<RespT> extends ClientCall.Listener<RespT> {

        private final ClientCall.Listener<RespT> delegate;

        ExceptionRestoringListener(ClientCall.Listener<RespT> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onHeaders(Metadata headers) {
            delegate.onHeaders(headers);
        }

        @Override
        public void onMessage(RespT message) {
            delegate.onMessage(message);
        }

        @Override
        public void onClose(Status status, Metadata trailers) {
            if (!status.isOk()) {
                String messageKey = trailers.get(GrpcExceptionMetadata.MESSAGE_KEY);
                if (messageKey != null && !messageKey.isBlank()) {
                    Map<String, Object> params =
                            GrpcExceptionParamsCodec.decode(trailers.get(GrpcExceptionMetadata.PARAMS));
                    delegate.onClose(status.withCause(new BusinessException(messageKey, params)), trailers);
                    return;
                }
            }
            delegate.onClose(status, trailers);
        }

        @Override
        public void onReady() {
            delegate.onReady();
        }
    }
}
