package com.kraft.common.web;

import com.kraft.common.error.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DeviceTokenSupport 단위 테스트")
class DeviceTokenSupportTest {

    private final DeviceTokenSupport support = new DeviceTokenSupport();

    private static final String VALID_TOKEN = "a".repeat(36); // 36-char UUID-length token

    @Test
    @DisplayName("정상 토큰은 SHA-256 해시를 반환한다")
    void requireHashedToken_validToken_returnsHash() {
        String hash = support.requireHashedToken(VALID_TOKEN);
        assertThat(hash).hasSize(64).matches("[0-9a-f]+");
    }

    @Test
    @DisplayName("null 토큰은 DEVICE_TOKEN_REQUIRED 예외를 발생시킨다")
    void requireHashedToken_null_throwsRequired() {
        assertThatThrownBy(() -> support.requireHashedToken(null))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("DEVICE_TOKEN_REQUIRED"));
    }

    @Test
    @DisplayName("빈 토큰은 DEVICE_TOKEN_REQUIRED 예외를 발생시킨다")
    void requireHashedToken_blank_throwsRequired() {
        assertThatThrownBy(() -> support.requireHashedToken("   "))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("DEVICE_TOKEN_REQUIRED"));
    }

    @Test
    @DisplayName("31자 토큰은 INVALID_DEVICE_TOKEN 예외를 발생시킨다")
    void requireHashedToken_tooShort_throwsInvalid() {
        assertThatThrownBy(() -> support.requireHashedToken("a".repeat(31)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getCode()).isEqualTo("INVALID_DEVICE_TOKEN");
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    @DisplayName("129자 토큰은 INVALID_DEVICE_TOKEN 예외를 발생시킨다")
    void requireHashedToken_tooLong_throwsInvalid() {
        assertThatThrownBy(() -> support.requireHashedToken("a".repeat(129)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("INVALID_DEVICE_TOKEN"));
    }

    @Test
    @DisplayName("32자 토큰은 경계값으로 허용된다")
    void requireHashedToken_exactly32_isValid() {
        assertThat(support.requireHashedToken("a".repeat(32))).hasSize(64);
    }

    @Test
    @DisplayName("128자 토큰은 경계값으로 허용된다")
    void requireHashedToken_exactly128_isValid() {
        assertThat(support.requireHashedToken("a".repeat(128))).hasSize(64);
    }

    @Test
    @DisplayName("동일한 토큰은 동일한 해시를 반환한다 (결정적)")
    void requireHashedToken_sameInput_sameHash() {
        assertThat(support.requireHashedToken(VALID_TOKEN))
                .isEqualTo(support.requireHashedToken(VALID_TOKEN));
    }
}
