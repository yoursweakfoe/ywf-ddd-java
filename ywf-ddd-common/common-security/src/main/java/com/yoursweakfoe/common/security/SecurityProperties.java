package com.yoursweakfoe.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * common-security 配置属性（record，构造器绑定）。
 *
 * <p>唯一的字段缝是「角色 → 权限」（{@code @PreAuthorize("hasRole(...)")} 需要），
 * 角色 claim 名可配置（默认 {@code roles}）。其余身份字段不写死——公司 JWT 字段命名 /
 * 数量无规范，由各服务从 {@code Jwt} claims 按需自取（{@link SecurityUtil#getString(String)} 等）。
 */
@ConfigurationProperties(prefix = "ywf.security")
public record SecurityProperties(
        /** JWT 中角色列表所在的 claim 名，默认 {@code roles}。 */
        @DefaultValue("roles") String rolesClaim) {
}