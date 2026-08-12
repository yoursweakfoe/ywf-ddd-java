package com.yoursweakfoe.common.exception;

import com.yoursweakfoe.common.exception.grpc.GrpcExceptionClientInterceptor;
import com.yoursweakfoe.common.exception.grpc.GrpcExceptionServerInterceptor;
import io.grpc.ClientInterceptor;
import io.grpc.ServerInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.client.GlobalClientInterceptor;
import org.springframework.grpc.server.GlobalServerInterceptor;

/**
 * 统一异常处理自动装配 —— Spring Boot AutoConfiguration 注册，引入依赖即生效。
 *
 * <p>按通道条件装配：
 * <ul>
 *   <li>REST 通道：{@link GlobalRestExceptionHandler}（Servlet Web 应用且存在 DispatcherServlet）
 *   <li>gRPC 通道：{@link GrpcExceptionServerInterceptor} / {@link GrpcExceptionClientInterceptor}
 *       （classpath 存在 grpc-api）
 * </ul>
 *
 * <p>gRPC interceptor {@code @Order(-50)}：晚于身份拦截器（-100）排序，
 * 即异常处理位于身份上下文建立的语义外层，两者职责互不干扰。
 */
@AutoConfiguration
public class ExceptionAutoConfiguration {

    /** REST 通道全局异常处理（RFC 9457）。 */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
    public GlobalRestExceptionHandler globalRestExceptionHandler() {
        return new GlobalRestExceptionHandler();
    }

    /** gRPC 通道异常处理组件（classpath 无 grpc-api 的纯 REST 服务不装配）。 */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({ServerInterceptor.class, GlobalServerInterceptor.class})
    static class GrpcExceptionConfiguration {

        /** 服务端：业务异常 → Status + Trailers。 */
        @Bean
        @Order(-50)
        @GlobalServerInterceptor
        ServerInterceptor grpcExceptionServerInterceptor() {
            return new GrpcExceptionServerInterceptor();
        }

        /** 客户端：Trailers → 还原 BusinessException（挂载 Status cause）。 */
        @Bean
        @Order(-50)
        @GlobalClientInterceptor
        ClientInterceptor grpcExceptionClientInterceptor() {
            return new GrpcExceptionClientInterceptor();
        }
    }
}
