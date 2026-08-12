package com.yoursweakfoe.common.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import io.grpc.ClientInterceptor;
import io.grpc.ServerInterceptor;
import org.apache.seata.integration.grpc.interceptor.client.ClientTransactionInterceptor;
import org.apache.seata.integration.grpc.interceptor.server.ServerTransactionInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@DisplayName("SeataGrpcAutoConfiguration — Seata gRPC 透传装配条件")
class SeataGrpcAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SeataGrpcAutoConfiguration.class));

    @Test
    @DisplayName("缺省（seata.enabled 未配置）：注册服务端与客户端事务拦截器")
    void defaultEnabled_registersBothInterceptors() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ServerTransactionInterceptor.class);
            assertThat(context).hasSingleBean(ClientTransactionInterceptor.class);
            // 同时以全局 interceptor 类型可见（供 spring-grpc 装配发现）
            assertThat(context.getBeansOfType(ServerInterceptor.class))
                    .containsKey("seataServerTransactionInterceptor");
            assertThat(context.getBeansOfType(ClientInterceptor.class))
                    .containsKey("seataClientTransactionInterceptor");
        });
    }

    @Test
    @DisplayName("seata.enabled=true：注册拦截器")
    void explicitlyEnabled_registersBothInterceptors() {
        contextRunner
                .withPropertyValues("seata.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(ServerTransactionInterceptor.class);
                    assertThat(context).hasSingleBean(ClientTransactionInterceptor.class);
                });
    }

    @Test
    @DisplayName("seata.enabled=false：不注册任何拦截器")
    void disabled_registersNothing() {
        contextRunner
                .withPropertyValues("seata.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ServerTransactionInterceptor.class);
                    assertThat(context).doesNotHaveBean(ClientTransactionInterceptor.class);
                });
    }
}
