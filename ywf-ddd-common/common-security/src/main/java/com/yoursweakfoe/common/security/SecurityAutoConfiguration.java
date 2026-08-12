package com.yoursweakfoe.common.security;

import com.yoursweakfoe.common.security.web.SecurityWebFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 身份上下文自动装配 —— Spring Boot AutoConfiguration 注册，引入依赖即生效。
 *
 * <p>REST 入站装配：
 * <ul>
 *   <li>REST 入站：{@link SecurityWebFilter}（仅 Servlet Web 应用）——
 *       声明为 Bean 后由 Boot 自动注册进 Servlet 过滤器链
 *   <li>REST 安全链：permit-all {@link SecurityFilterChain}（见下）
 * </ul>
 *
 * <p><b>permit-all 安全链（D12 边界语义）：</b>本框架的身份模型中，JWT 验签与
 * 鉴权决策统一收口在网关（Higress jwt-auth），服务层 REST 边界**仅解析**网关注入的
 * 身份 Header，不做重复验签。因此默认提供 permit-all + 无状态的 SecurityFilterChain，
 * 覆盖 Boot 安全自动配置的默认鉴权链（默认链会要求认证并拦截所有端点）。
 * 服务如需自定义安全链，声明自己的 {@link SecurityFilterChain} Bean 即可覆盖。
 *
 * <p>东西向 HTTP 身份传播为未来设计，当前身份仅在网关边界解析。
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
}
