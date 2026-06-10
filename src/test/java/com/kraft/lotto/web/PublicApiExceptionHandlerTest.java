package com.kraft.lotto.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kraft.lotto.support.ApiResponse;
import com.kraft.lotto.support.BusinessException;
import com.kraft.lotto.support.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@DisplayName("공개 API 예외 핸들러")
class PublicApiExceptionHandlerTest {

    private final PublicApiExceptionHandler handler = new PublicApiExceptionHandler();

    @Test
    @DisplayName("4백 번대 비즈니스 예외는 클라이언트 메시지를 그대로 반환한다")
    void handles4xxBusinessExceptionWithOriginalMessage() {
        var ex = new BusinessException(ErrorCode.WINNING_NUMBER_NOT_FOUND, "1200회차를 찾을 수 없습니다.");

        var response = handler.handleBusiness(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().error().message()).isEqualTo("1200회차를 찾을 수 없습니다.");
        assertThat(response.getBody().error().code()).isEqualTo(ErrorCode.WINNING_NUMBER_NOT_FOUND.name());
    }

    @Test
    @DisplayName("5백 번대 비즈니스 예외는 기본 메시지로 마스킹된다")
    void handles5xxBusinessExceptionWithDefaultMessage() {
        var ex = new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "내부 상세 정보");

        var response = handler.handleBusiness(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().error().message())
                .isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage());
    }

    @Test
    @DisplayName("메서드 인자 타입 불일치는 400으로 처리한다")
    void handlesTypeMismatchAs400() {
        var ex = mock(MethodArgumentTypeMismatchException.class);

        var response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().error().code())
                .isEqualTo(ErrorCode.REQUEST_VALIDATION_ERROR.name());
    }

    @Test
    @DisplayName("제약 조건 위반은 400으로 처리한다")
    void handlesConstraintViolationAs400() {
        var ex = new ConstraintViolationException(Set.of());

        var response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("요청 바디 검증 실패는 400으로 처리한다")
    void handlesMethodArgumentNotValidAs400() {
        var ex = mock(MethodArgumentNotValidException.class);

        var response = handler.handleMethodArgumentNotValid(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().error().code())
                .isEqualTo(ErrorCode.REQUEST_VALIDATION_ERROR.name());
        assertThat(response.getBody().error().message()).isEqualTo("invalid request body");
    }

    @Test
    @DisplayName("읽을 수 없는 요청 바디는 400으로 처리한다")
    void handlesUnreadableBodyAs400() {
        var ex = mock(HttpMessageNotReadableException.class);

        var response = handler.handleUnreadable(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().error().code())
                .isEqualTo(ErrorCode.REQUEST_VALIDATION_ERROR.name());
    }

    @Test
    @DisplayName("허용되지 않은 HTTP 메서드는 405로 처리한다")
    void handlesMethodNotAllowedAs405() {
        var ex = new HttpRequestMethodNotSupportedException("DELETE");

        var response = handler.handleMethodNotAllowed(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody().error().code())
                .isEqualTo(ErrorCode.METHOD_NOT_ALLOWED.name());
    }

    @Test
    @DisplayName("존재하지 않는 경로는 404로 처리한다")
    void handlesNoResourceAs404() {
        var ex = mock(NoResourceFoundException.class);

        var response = handler.handleNoResource(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().error().code())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.name());
    }

    @Test
    @DisplayName("처리되지 않은 예외는 500으로 처리하고 상세 정보를 노출하지 않는다")
    void handlesUnexpectedExceptionAs500() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getMethod()).thenReturn("GET");
        when(req.getRequestURI()).thenReturn("/api/v1/rounds/latest");

        var response = handler.handleUnexpected(new RuntimeException("DB 연결 실패"), req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.error().message())
                .isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage());
        assertThat(body.error().message()).doesNotContain("DB 연결 실패");
    }

    @Test
    @DisplayName("재시도 가능 오류는 retryable=true를 반환한다")
    void retryableErrorCodeHasRetryableTrueInBody() {
        var ex = new BusinessException(ErrorCode.LOTTO_GENERATION_TIMEOUT, "timeout");

        var response = handler.handleBusiness(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().error().retryable()).isTrue();
    }
}
