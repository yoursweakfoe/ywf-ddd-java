package com.yoursweakfoe.common.security;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 真实资源服务器链集成测试 —— 以公司现状（对称 HS256 + 字段命名/数量无规范）为对接基准：
 * 数值 {@code uid}、非标 {@code uname}、任意字段 {@code department}、可能缺失的字段，
 * 验证零信任装配端到端：验签 + 原生 {@link Jwt} + 权限映射 + {@code @PreAuthorize}。
 */
@DisplayName("ResourceServer — 零信任 JWT 集成（字段无规范基准）")
@SpringBootTest(classes = ResourceServerIntegrationTest.TestConfig.class)
@AutoConfigureMockMvc
class ResourceServerIntegrationTest {

    private static final byte[] SECRET = "this-is-a-test-secret-key-for-hmac-sha256".getBytes(StandardCharsets.UTF_8);

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(IdentityController.class)
    static class TestConfig {

        @Bean
        JwtDecoder jwtDecoder() {
            return NimbusJwtDecoder.withSecretKey(secretKey()).macAlgorithm(MacAlgorithm.HS256).build();
        }

        @Bean
        JwtEncoder jwtEncoder() {
            return new NimbusJwtEncoder(new ImmutableSecret<>(SECRET));
        }

        private static SecretKey secretKey() {
            return new SecretKeySpec(SECRET, "HmacSHA256");
        }
    }

    @RestController
    static class IdentityController {

        @GetMapping("/identity/claims")
        Map<String, Object> claims(@AuthenticationPrincipal Jwt jwt) {
            if (jwt == null) {
                return Map.of("authenticated", false);
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("authenticated", true);
            result.put("uid", SecurityUtil.getString("uid"));               // 数值 → 字符串
            result.put("uname", SecurityUtil.getString("uname"));           // 非标字段
            result.put("department", SecurityUtil.getString("department")); // 任意字段（可能没有）
            result.put("roles", SecurityUtil.getStringList("roles"));
            return result;
        }

        @GetMapping("/identity/util")
        String util() {
            return SecurityUtil.getString("uid");
        }

        @GetMapping("/identity/admin")
        @PreAuthorize("hasRole('ADMIN')")
        String admin() {
            return "ok";
        }
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtEncoder jwtEncoder;

    /** 公司现状：字段命名 / 数量随意。 */
    private String token(Map<String, Object> claims) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
                .issuer("test")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300));
        claims.forEach(builder::claim);
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, builder.build())).getTokenValue();
    }

    @Test
    @DisplayName("匿名请求（无 token）：身份为 null")
    void anonymousRequest_returnsNullIdentity() throws Exception {
        mockMvc.perform(get("/identity/claims"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false))
                .andExpect(jsonPath("$.uid").doesNotExist());

        mockMvc.perform(get("/identity/util"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("字段无规范 token：数值 uid + 非标 uname + 任意 department 都能读")
    void arbitraryClaims_readByName() throws Exception {
        String token = token(Map.of(
                "uid", 123456L,
                "uname", "zhangsan",
                "department", "研发部",
                "roles", List.of("ADMIN", "USER")));

        mockMvc.perform(get("/identity/claims").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.uid").value("123456"))
                .andExpect(jsonPath("$.uname").value("zhangsan"))
                .andExpect(jsonPath("$.department").value("研发部"))
                .andExpect(jsonPath("$.roles").value(containsInAnyOrder("ADMIN", "USER")));
    }

    @Test
    @DisplayName("只有 userId 没有用户名 / 部门的 token：缺失字段读 null，不炸")
    void minimalClaims_missingFieldsReadNull() throws Exception {
        String token = token(Map.of("uid", 888L));

        mockMvc.perform(get("/identity/claims").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid").value("888"))
                .andExpect(jsonPath("$.uname").value((String) null))
                .andExpect(jsonPath("$.department").value((String) null))
                .andExpect(jsonPath("$.roles").isEmpty());
    }

    @Test
    @DisplayName("@PreAuthorize hasRole 生效：无 ADMIN 角色 → 403，有 ADMIN → 200")
    void preAuthorize_enforcesRole() throws Exception {
        String userToken = token(Map.of("uid", 1L, "roles", List.of("USER")));
        mockMvc.perform(get("/identity/admin").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        String adminToken = token(Map.of("uid", 1L, "roles", List.of("ADMIN")));
        mockMvc.perform(get("/identity/admin").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    @DisplayName("非法 token → 401（fail-closed，不像旧框架静默放行）")
    void invalidToken_returns401() throws Exception {
        mockMvc.perform(get("/identity/claims").header("Authorization", "Bearer not-a-valid-jwt"))
                .andExpect(status().isUnauthorized());
    }
}
