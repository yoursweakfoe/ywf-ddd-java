package com.yoursweakfoe.common.cloud.feign.interceptor;

import com.yoursweakfoe.common.cloud.feign.CommonCloudProperties;
import com.yoursweakfoe.common.security.context.SecurityUtil;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.Target;
import java.net.URI;
import java.util.List;
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
 *
 * <p>三个可配置行为（见 {@link CommonCloudProperties}）：
 * <ul>
 *   <li>全局开关 {@code enabled=false} 时完全不做透传；</li>
 *   <li>Header 名与令牌前缀可自定义；</li>
 *   <li>{@code exclude-hosts} 命中的目标主机不注入，避免内网 JWT 泄漏给第三方
 *       Feign 调用目标（如外呼支付 / 短信网关）。</li>
 * </ul>
 */
public class JwtPropagationRequestInterceptor implements RequestInterceptor {

    private final CommonCloudProperties properties;

    public JwtPropagationRequestInterceptor(CommonCloudProperties properties) {
        this.properties = properties;
    }

    @Override
    public void apply(RequestTemplate template) {
        if (!properties.enabled() || isExcluded(template)) {
            return;
        }
        Jwt jwt = SecurityUtil.getJwt();
        if (jwt != null) {
            template.header(properties.headerName(), properties.prefix() + jwt.getTokenValue());
        }
    }

    /** 目标主机是否命中免透传列表。列表为空或目标无法解析时一律不排除（安全默认 = 仍透传）。 */
    private boolean isExcluded(RequestTemplate template) {
        List<String> excludeHosts = properties.excludeHosts();
        if (excludeHosts == null || excludeHosts.isEmpty()) {
            return false;
        }
        String host = resolveHost(template);
        return host != null && excludeHosts.contains(host);
    }

    /** 从 Feign Target 解析目标主机名；解析失败回退到 target 名，再失败返回 null。 */
    private String resolveHost(RequestTemplate template) {
        Target<?> target = template.feignTarget();
        if (target == null) {
            return null;
        }
        String url = target.url();
        if (url != null && !url.isBlank()) {
            try {
                String host = URI.create(url).getHost();
                if (host != null) {
                    return host;
                }
            } catch (IllegalArgumentException ignored) {
                // url 不是合法 URI（如裸服务名），回退到 target name
            }
        }
        return target.name();
    }
}
