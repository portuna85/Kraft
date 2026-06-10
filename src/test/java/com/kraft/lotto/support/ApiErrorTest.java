package com.kraft.lotto.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("에이피아이 오류 응답")
class ApiErrorTest {

    @Test
    @DisplayName("오류 코드 팩토리는 기본 메시지를 사용한다")
    void ofWithCodeOnly() {
        ApiError error = ApiError.of(ErrorCode.RESOURCE_NOT_FOUND);

        assertThat(error.code()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(error.message()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.getDefaultMessage());
        assertThat(error.retryable()).isFalse();
    }

    @Test
    @DisplayName("오류 코드와 메시지 팩토리는 유효한 메시지면 그대로 사용한다")
    void ofWithCustomMessage() {
        ApiError error = ApiError.of(ErrorCode.REQUEST_VALIDATION_ERROR, "round must be positive");

        assertThat(error.message()).isEqualTo("round must be positive");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("오류 코드와 메시지 팩토리는 널·빈·공백 메시지를 기본값으로 대체한다")
    void ofWithBlankMessageFallsBackToDefault(String msg) {
        ApiError error = ApiError.of(ErrorCode.REQUEST_VALIDATION_ERROR, msg);

        assertThat(error.message()).isEqualTo(ErrorCode.REQUEST_VALIDATION_ERROR.getDefaultMessage());
    }

    @Test
    @DisplayName("재시도 가능 오류 코드는 재시도 가능 참을 반환한다")
    void retryableErrorCode() {
        ApiError error = ApiError.of(ErrorCode.LOTTO_GENERATION_TIMEOUT);

        assertThat(error.retryable()).isTrue();
    }
}
