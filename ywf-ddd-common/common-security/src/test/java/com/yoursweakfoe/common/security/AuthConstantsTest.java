package com.yoursweakfoe.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.yoursweakfoe.common.security.AuthConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AuthConstants — 认证 Header 常量测试")
class AuthConstantsTest {

    @Test
    void HDR_USER_ID_notNullNotEmpty() {
        assertThat(AuthConstants.HDR_USER_ID).isNotNull().isNotBlank();
    }

    @Test
    void HDR_USERNAME_notNullNotEmpty() {
        assertThat(AuthConstants.HDR_USERNAME).isNotNull().isNotBlank();
    }

    @Test
    void HDR_ROLES_notNullNotEmpty() {
        assertThat(AuthConstants.HDR_ROLES).isNotNull().isNotBlank();
    }

    @Test
    void HDR_USER_ID_hasExpectedValue() {
        assertThat(AuthConstants.HDR_USER_ID).isEqualTo("X-User-Id");
    }

    @Test
    void HDR_USERNAME_hasExpectedValue() {
        assertThat(AuthConstants.HDR_USERNAME).isEqualTo("X-Username");
    }

    @Test
    void HDR_ROLES_hasExpectedValue() {
        assertThat(AuthConstants.HDR_ROLES).isEqualTo("X-Roles");
    }
}
