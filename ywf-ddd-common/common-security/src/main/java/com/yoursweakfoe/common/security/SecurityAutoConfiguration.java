package com.yoursweakfoe.common.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 零信任身份自动装配 —— 资源服务器验签（{@code JwtDecoder} 由消费方提供）+ 方法级鉴权。
 *
 * <p>三件事：角色 claim（名可配，默认 {@code roles}）→ {@code ROLE_*} 权限；
 * permit-all 无状态链（路由鉴权在网关）；{@code @EnableMethodSecurity}。
 * {@code beforeName} 让本链先于 Boot 默认链注册、使其退避。
 */
@AutoConfiguration(beforeName = "org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration")
@EnableConfigurationProperties(SecurityProperties.class)
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityAutoConfiguration {

    /** 角色 claim（名可配）→ {@code ROLE_*} 权限；principal 保持原生 {@code Jwt}。 */
    @Bean
    @ConditionalOnMissingBean
    JwtAuthenticationConverter jwtAuthenticationConverter(SecurityProperties properties) {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(properties.getRolesClaim());
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    /** permit-all + 无状态 + 资源服务器链。 */
    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter converter) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
