package com.yoursweakfoe.common.cloud.feign;

import com.yoursweakfoe.common.security.SecurityUtil;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * 东西向 JWT 身份传播 —— 把当前请求已验签的 JWT 透传到下游 Feign 调用。
 *
 * <p>零信任下服务间不靠内网可信，也不靠网关注入身份 Header；而是把当前线程里
 * 已验签的 {@link Jwt} 原样带上 {@code Authorization: Bearer} 透传给下游，下游
 * 作为资源服务器（common-security）自行验签。
 *
 * <p>仅在当前线程存在已认证身份时透传（即用户请求路径）；匿名 / 定时任务 / MQ 等
 * 无用户上下文的调用不注入（机器身份需走 client-credentials，另行设计）。
 */
public class JwtPropagationRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        Jwt jwt = SecurityUtil.getJwt();
        if (jwt != null) {
            template.header("Authorization", "Bearer " + jwt.getTokenValue());
        }
    }
}
