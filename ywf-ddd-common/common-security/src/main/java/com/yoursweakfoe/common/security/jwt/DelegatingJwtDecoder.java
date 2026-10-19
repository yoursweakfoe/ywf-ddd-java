package com.yoursweakfoe.common.security.jwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.util.Base64URL;
import java.util.Map;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * 多验签方案分发器 —— 按 JOSE 头的 {@code alg} 路由到对应的 {@link JwtDecoder}。
 *
 * <p>用于「不同来源 JWT 使用不同签名算法」：读头部 {@code alg}（不验签）选 decoder 再验签。
 * 匹配不到 {@code alg} 时拒绝（fail-closed）。可选工具类，谁需要谁 {@code new}。
 *
 * <pre>{@code
 * JwtDecoder decoder = new DelegatingJwtDecoder(Map.of(
 *     "HS256", NimbusJwtDecoder.withSecretKey(hmacKey).macAlgorithm(HS256).build(),
 *     "RS256", NimbusJwtDecoder.withJwkSetUri(jwksUrl).build()
 * ));
 * }</pre>
 */
public class DelegatingJwtDecoder implements JwtDecoder {

    private final Map<String, JwtDecoder> delegates;

    public DelegatingJwtDecoder(Map<String, JwtDecoder> delegates) {
        this.delegates = Map.copyOf(delegates);
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        String alg = headerAlgorithm(token);
        JwtDecoder decoder = delegates.get(alg);
        if (decoder == null) {
            throw new BadJwtException("Unsupported signature algorithm: " + alg);
        }
        return decoder.decode(token);
    }

    /** 只读 JOSE 头取 {@code alg}（不验签、不解析 payload）。 */
    private static String headerAlgorithm(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                throw new BadJwtException("Malformed JWT: missing signature segment");
            }
            JWSHeader header = JWSHeader.parse(new Base64URL(parts[0]));
            JWSAlgorithm alg = header.getAlgorithm();
            if (alg == null) {
                throw new BadJwtException("JWT header missing alg");
            }
            return alg.getName();
        } catch (BadJwtException e) {
            throw e;
        } catch (Exception e) {
            throw new BadJwtException("Failed to parse JWT header", e);
        }
    }
}
