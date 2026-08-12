package com.yoursweakfoe.common.security;

import com.yoursweakfoe.common.security.grpc.GrpcSecurityClientInterceptor;
import com.yoursweakfoe.common.security.grpc.GrpcSecurityServerInterceptor;
import com.yoursweakfoe.common.security.web.SecurityWebFilter;
import io.grpc.ClientInterceptor;
import io.grpc.ServerInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.client.GlobalClientInterceptor;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 身份上下文自动装配 —— Spring Boot AutoConfiguration 注册，引入依赖即生效。
 *
 * <p>按通道条件装配：
 * <ul>
 *   <li>REST 入站：{@link SecurityWebFilter}（仅 Servlet Web 应用）——
 *       声明为 Bean 后由 Boot 自动注册进 Servlet 过滤器链
 *   <li>REST 安全链：permit-all {@link SecurityFilterChain}（见下）
 *   <li>gRPC 入站：{@link GrpcSecurityServerInterceptor}（仅 classpath 存在 grpc-api）
 *   <li>gRPC 出站：{@link GrpcSecurityClientInterceptor}（仅 classpath 存在 grpc-api）
 * </ul>
 *
 * <p><b>permit-all 安全链（D12 边界语义）：</b>本框架的身份模型中，JWT 验签与
 * 鉴权决策统一收口在网关（Higress jwt-auth），服务层 REST 边界**仅解析**网关注入的
 * 身份 Header，不做重复验签。因此默认提供 permit-all + 无状态的 SecurityFilterChain，
 * 覆盖 Boot 安全自动配置的默认鉴权链（默认链会要求认证并拦截所有端点）。
 * 服务如需自定义安全链，声明自己的 {@link SecurityFilterChain} Bean 即可覆盖。
 *
 * <p>全局 interceptor 经 spring-grpc 的 {@code @GlobalServerInterceptor} /
 * {@code @GlobalClientInterceptor} 注解注册，作用于所有服务/通道；
 * {@code @Order(-100)} 保证身份上下文先于常规业务 interceptor 建立。
 */
@AutoConfiguration(beforeName = "org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration")
public class SecurityAutoConfiguration {

    /** REST 入站身份解析 Filter（网关 Header → SecurityContext，source=edge）。 */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public SecurityWebFilter securityWebFilter() {
        return new SecurityWebFilter();
    }

    /**
     * REST 安全链装配（仅 Servlet Web 应用）。
     *
     * <p>边界 permit-all 安全链：鉴权在网关，服务层仅解析身份。
     * CSRF 关闭（无浏览器会话，网关已验签）；会话无状态（身份逐请求解析）。
     * 先于 Boot 的 ServletWebSecurityAutoConfiguration 注册（见类级 beforeName），
     * 使 Boot 的默认鉴权链（要求认证）因 {@code @ConditionalOnMissingBean} 退避。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @EnableWebSecurity
    static class WebSecurityChainConfiguration {

        @Bean
        @ConditionalOnMissingBean(SecurityFilterChain.class)
        SecurityFilterChain permitAllSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    /** gRPC 身份传播组件（classpath 无 grpc-api 的纯 REST 服务不装配）。 */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({ServerInterceptor.class, GlobalServerInterceptor.class})
    static class GrpcSecurityConfiguration {

        /** gRPC 入站：Metadata → SecurityContext（source=propagated）。 */
        @Bean
        @Order(-100)
        @GlobalServerInterceptor
        ServerInterceptor grpcSecurityServerInterceptor() {
            return new GrpcSecurityServerInterceptor();
        }

        /** gRPC 出站：SecurityContext → Metadata。 */
        @Bean
        @Order(-100)
        @GlobalClientInterceptor
        ClientInterceptor grpcSecurityClientInterceptor() {
            return new GrpcSecurityClientInterceptor();
        }
    }
}
