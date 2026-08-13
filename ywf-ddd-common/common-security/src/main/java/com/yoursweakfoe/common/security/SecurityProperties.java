package com.yoursweakfoe.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * common-security 配置属性。
 *
 * <p>唯一的字段缝是「角色 → 权限」（{@code @PreAuthorize("hasRole(...)")} 需要），
 * 角色 claim 名可配置（默认 {@code roles}）。其余身份字段不写死——公司 JWT 字段命名 /
 * 数量无规范，由各服务从 {@code Jwt} claims 按需自取（{@link SecurityUtil#getString(String)} 等）。
 */
@ConfigurationProperties(prefix = "ywf.security")
public class SecurityProperties {

    /** JWT 中角色列表所在的 claim 名，默认 {@code roles}。 */
    private String rolesClaim = "roles";

    public String getRolesClaim() {
        return rolesClaim;
    }

    public void setRolesClaim(String rolesClaim) {
        this.rolesClaim = rolesClaim;
    }
}
