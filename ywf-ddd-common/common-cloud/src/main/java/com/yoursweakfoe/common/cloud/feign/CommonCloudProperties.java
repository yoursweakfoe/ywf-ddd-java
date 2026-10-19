package com.yoursweakfoe.common.cloud.feign;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * common-cloud 东西向 JWT 身份传播配置（record，构造器绑定）。
 *
 * <p>控制 {@code JwtPropagationRequestInterceptor} 如何把当前已验签的 JWT 透传给下游 Feign 调用。
 * 前缀 {@code ywf.cloud.feign.jwt}：
 *
 * <ul>
 *   <li>{@code enabled} —— 全局开关（默认开启）。设为 {@code false} 时完全不做身份透传
 *       （匿名 / 定时任务 / MQ 等本就无用户上下文，与关闭效果相同，但该开关面向「明确不想要透传」的场景）。</li>
 *   <li>{@code header-name} —— 透传的 Header 名（默认 {@code Authorization}）。</li>
 *   <li>{@code prefix} —— 令牌前缀（默认 {@code "Bearer "}，含尾随空格）。</li>
 *   <li>{@code exclude-hosts} —— 免透传的主机列表。命中时不注入 Header，
 *       避免内网 JWT 泄漏给第三方 Feign 调用目标（如外呼支付 / 短信网关）。匹配目标主机的 host 名。</li>
 * </ul>
 *
 * <pre>{@code
 * ywf:
 *   cloud:
 *     feign:
 *       jwt:
 *         enabled: true                      # 默认 true
 *         header-name: Authorization         # 默认 Authorization
 *         prefix: "Bearer "                  # 默认 "Bearer "（含尾随空格）
 *         exclude-hosts:
 *           - api.external-payment.com
 *           - sms-gateway.internal
 * }</pre>
 */
@ConfigurationProperties(prefix = "ywf.cloud.feign.jwt")
public record CommonCloudProperties(
        /** 身份透传全局开关，默认 {@code true} */
        @DefaultValue("true") boolean enabled,
        /** 透传 Header 名，默认 {@code Authorization} */
        @DefaultValue("Authorization") String headerName,
        /** 令牌前缀，默认 {@code "Bearer "}（含尾随空格） */
        @DefaultValue("Bearer ") String prefix,
        /** 免透传的目标主机列表，为空时不排除任何主机 */
        List<String> excludeHosts) {
}