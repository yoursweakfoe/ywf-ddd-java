package com.yoursweakfoe.common.security.grpc;

import com.yoursweakfoe.common.security.IdentitySource;
import com.yoursweakfoe.common.security.SecurityContextSupport;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import org.springframework.security.core.Authentication;

/**
 * gRPC 入站安全拦截器 —— 从 Metadata 读取上游传播的用户身份，
 * 在业务回调执行期间建立 SecurityContext（source=propagated）。
 *
 * <p><b>职责边界（gRPC 入站通道）：</b>本拦截器只负责 <b>gRPC 入站</b>身份重建，
 * 身份来源唯一为 gRPC Metadata（{@code sec_user_id} 等）。
 * REST 入站由 {@code SecurityWebFilter} 独立负责，两者互不依赖。
 *
 * <p><b>线程模型：</b>gRPC 的业务回调（{@code onMessage} / {@code onHalfClose} 等）
 * 由传输层线程池调度，不一定与 {@code interceptCall} 同线程。因此身份上下文
 * 通过 {@link SecurityContextListenerProxy} 在<b>每个回调前后</b>建立/清理，
 * 而非在 {@code interceptCall} 中一次性建立——保证业务代码无论落在哪个回调线程
 * 都能经 {@code SecurityUtil} 读到身份。
 *
 * <p><b>多跳传播（A → B → C）：</b>B 的业务代码运行在已建立上下文的回调内，
 * 其发起的下游调用由 {@code GrpcSecurityClientInterceptor} 从 SecurityContext
 * 读取身份重新写入 Metadata——多跳透传逐跳自动完成。
 *
 * <p>Metadata 无身份信息时（匿名调用）直接透传，不建立也不清理上下文。
 *
 * <p>注册：{@code SecurityAutoConfiguration} 以 {@code @GlobalServerInterceptor} Bean 装配。
 */
public class GrpcSecurityServerInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        String userId = headers.get(SecurityMetadata.USER_ID);

        // 无 gRPC 身份（匿名调用）：透传，不触碰 SecurityContext
        if (userId == null || userId.isBlank()) {
            return next.startCall(call, headers);
        }

        Authentication authentication = SecurityContextSupport.buildAuthentication(
                userId,
                headers.get(SecurityMetadata.USERNAME),
                headers.get(SecurityMetadata.ROLES),
                IdentitySource.PROPAGATED);
        return new SecurityContextListenerProxy<>(next.startCall(call, headers), authentication);
    }

    /**
     * 监听器代理 —— 在每个业务回调周围建立/清理 SecurityContext。
     *
     * <p>覆盖全部五个回调入口：一元调用的业务执行发生在 {@code onHalfClose}，
     * 流式调用的消息处理发生在 {@code onMessage}，其余回调同样可能承载用户代码。
     */
    static final class SecurityContextListenerProxy<ReqT>
            extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {

        private final Authentication authentication;

        SecurityContextListenerProxy(ServerCall.Listener<ReqT> delegate, Authentication authentication) {
            super(delegate);
            this.authentication = authentication;
        }

        @Override
        public void onMessage(ReqT message) {
            runWithContext(() -> super.onMessage(message));
        }

        @Override
        public void onHalfClose() {
            runWithContext(super::onHalfClose);
        }

        @Override
        public void onCancel() {
            runWithContext(super::onCancel);
        }

        @Override
        public void onComplete() {
            runWithContext(super::onComplete);
        }

        @Override
        public void onReady() {
            runWithContext(super::onReady);
        }

        private void runWithContext(Runnable action) {
            SecurityContextSupport.establish(authentication);
            try {
                action.run();
            } finally {
                SecurityContextSupport.clear();
            }
        }
    }
}
