package com.kraft.lotto.infra.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("시작 검증 예외")
class StartupValidationExceptionTest {

    @Test
    @DisplayName("메시지를 보존하며 스택 추적 채우기는 자기 자신을 반환한다")
    void messageAndNoStackTrace() {
        StartupValidationException ex = new StartupValidationException("missing env");

        assertThat(ex.getMessage()).isEqualTo("missing env");
        assertThat(ex.fillInStackTrace()).isSameAs(ex);
        assertThat(ex.getStackTrace()).isEmpty();
    }
}
