package com.kraft.lotto.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("API 오류 응답")
class ApiErrorTest {

    @Test
    @DisplayName("of(ErrorCode) — 기본 메시지를 사용한다")
    void ofWithCodeOnly() {
        ApiError error = ApiError.of(ErrorCode.RESOURCE_NOT_FOUND);

        assertThat(error.code()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(error.message()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.getDefaultMessage());
        assertThat(error.retryable()).isFalse();
    }

    @Test
    @DisplayName("of(ErrorCode, message) — 유효한 메시지면 그대로 사용한다")
    void ofWithCustomMessage() {
        ApiError error = ApiError.of(ErrorCode.REQUEST_VALIDATION_ERROR, "round must be positive");

        assertThat(error.message()).isEqualTo("round must be positive");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("of(ErrorCode, message) — null·빈·공백 메시지는 기본값으로 대체된다")
    void ofWithBlankMessageFallsBackToDefault(String msg) {
        ApiError error = ApiError.of(ErrorCode.REQUEST_VALIDATION_ERROR, msg);

        assertThat(error.message()).isEqualTo(ErrorCode.REQUEST_VALIDATION_ERROR.getDefaultMessage());
    }

    @Test
    @DisplayName("retryable ErrorCode는 retryable=true를 반환한다")
    void retryableErrorCode() {
        ApiError error = ApiError.of(ErrorCode.LOTTO_GENERATION_TIMEOUT);

        assertThat(error.retryable()).isTrue();
    }
}
