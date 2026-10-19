package com.yoursweakfoe.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * common-security 配置属性（record，构造器绑定）。
 *
 * <p>两个配置项：{@code enabled}（安全链总开关）与 {@code roles-claim}（角色 claim 名）、
 * {@code authority-prefix}（角色 → 权限前缀）。角色 claim 名可配置（默认 {@code roles}），
 * 权限前缀可配置（默认 {@code ROLE_}）。其余身份字段不写死——公司 JWT 字段命名 /
 * 数量无规范，由各服务从 {@code Jwt} claims 按需自取（{@link com.yoursweakfoe.common.security.context.SecurityUtil#getString(String)} 等）。
 */
@ConfigurationProperties(prefix = "ywf.security")
public record SecurityProperties(
        /** 安全链总开关，默认 {@code true}（缺省启用，向后兼容）。设为 {@code false} 时整条安全链不注册。 */
        @DefaultValue("true") boolean enabled,
        /** JWT 中角色列表所在的 claim 名，默认 {@code roles}。 */
        @DefaultValue("roles") String rolesClaim,
        /** 角色 → 权限前缀，默认 {@code "ROLE_"}（Spring Security hasRole 约定）。 */
        @DefaultValue("ROLE_") String authorityPrefix) {
}