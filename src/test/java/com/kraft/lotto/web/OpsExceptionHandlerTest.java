package com.kraft.lotto.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.kraft.lotto.support.ApiResponse;
import com.kraft.lotto.support.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;

@DisplayName("Ops 예외 핸들러 테스트")
class OpsExceptionHandlerTest {

    private final OpsExceptionHandler handler = new OpsExceptionHandler();

    @Test
    @DisplayName("허용되지 않은 HTTP 메서드는 405 응답으로 처리한다")
    void handlesMethodNotAllowedAs405() {
        var ex = new HttpRequestMethodNotSupportedException("POST");
        var response = handler.handleMethodNotAllowed(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.success()).isFalse();
        assertThat(body.error()).isNotNull();
        assertThat(body.error().code()).isEqualTo(ErrorCode.METHOD_NOT_ALLOWED.name());
    }
}
