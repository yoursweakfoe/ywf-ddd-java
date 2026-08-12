package com.yoursweakfoe.common.cloud;

import io.grpc.ClientInterceptor;
import io.grpc.ServerInterceptor;
import org.apache.seata.integration.grpc.interceptor.client.ClientTransactionInterceptor;
import org.apache.seata.integration.grpc.interceptor.server.ServerTransactionInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.client.GlobalClientInterceptor;
import org.springframework.grpc.server.GlobalServerInterceptor;

/**
 * Seata gRPC XID 透传自动装配 —— 全局 interceptor 注册 Seata 官方 gRPC 集成。
 *
 * <p>将 Seata 官方 {@code seata-grpc} 构件提供的事务拦截器注册为全局 gRPC interceptor：
 * <ul>
 *   <li>服务端 {@link ServerTransactionInterceptor}：从入站 Metadata 还原 XID 绑定至 RootContext，
 *       分支注册随本地事务自动完成</li>
 *   <li>客户端 {@link ClientTransactionInterceptor}：出站调用时将当前 RootContext 的 XID 写入 Metadata</li>
 * </ul>
 *
 * <p>装配条件：classpath 存在 seata-grpc 与 grpc-api，且 Seata 处于启用态
 * （{@code seata.enabled} 缺省为 true，与 Seata 自身自动装配语义一致）。
 *
 * <p>{@code @Order(-200)}：早于身份（-100）与异常（-50）拦截器排序，
 * 保证 XID 上下文在最内层建立/清理，不受其余拦截器的异常处理影响。
 */
@AutoConfiguration
@ConditionalOnClass({ServerTransactionInterceptor.class, GlobalServerInterceptor.class})
@ConditionalOnProperty(prefix = "seata", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SeataGrpcAutoConfiguration {

    /** 服务端：入站 XID 还原（Metadata → RootContext）。 */
    @Bean
    @Order(-200)
    @GlobalServerInterceptor
    ServerInterceptor seataServerTransactionInterceptor() {
        return new ServerTransactionInterceptor();
    }

    /** 客户端：出站 XID 传递（RootContext → Metadata）。 */
    @Bean
    @Order(-200)
    @GlobalClientInterceptor
    ClientInterceptor seataClientTransactionInterceptor() {
        return new ClientTransactionInterceptor();
    }
}
