package com.yoursweakfoe.common.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@DisplayName("DelegatingJwtDecoder — 按 alg 分发验签")
class DelegatingJwtDecoderTest {

    private static String tokenWithAlg(String alg) {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"alg\":\"" + alg + "\"}").getBytes(StandardCharsets.UTF_8));
        return header + ".eyJzdWIiOiIxIn0.sig";
    }

    private static Jwt jwt(String tokenValue) {
        return Jwt.withTokenValue(tokenValue)
                .header("alg", "HS256")
                .subject("1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }

    @Test
    void routesByAlg() {
        String hsToken = tokenWithAlg("HS256");
        JwtDecoder hmac = mock(JwtDecoder.class);
        JwtDecoder rsa = mock(JwtDecoder.class);
        Jwt expected = jwt(hsToken);
        when(hmac.decode(hsToken)).thenReturn(expected);

        DelegatingJwtDecoder decoder = new DelegatingJwtDecoder(Map.of("HS256", hmac, "RS256", rsa));

        assertThat(decoder.decode(hsToken)).isSameAs(expected);
        verify(hmac).decode(hsToken);
        verifyNoInteractions(rsa);
    }

    @Test
    void unknownAlg_rejected() {
        JwtDecoder hmac = mock(JwtDecoder.class);
        DelegatingJwtDecoder decoder = new DelegatingJwtDecoder(Map.of("HS256", hmac));

        assertThatThrownBy(() -> decoder.decode(tokenWithAlg("ES256")))
                .isInstanceOf(BadJwtException.class);
        verifyNoInteractions(hmac);
    }
}
