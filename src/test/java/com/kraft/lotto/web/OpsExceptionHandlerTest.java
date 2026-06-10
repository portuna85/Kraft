package com.kraft.lotto.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kraft.lotto.support.ApiResponse;
import com.kraft.lotto.support.BusinessException;
import com.kraft.lotto.support.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@DisplayName("운영 예외 핸들러 테스트")
class OpsExceptionHandlerTest {

    private final OpsExceptionHandler handler = new OpsExceptionHandler();

    @Test
    @DisplayName("허용되지 않은 에이치티티피 메서드는 405 응답으로 처리한다")
    void handlesMethodNotAllowedAs405() {
        var ex = new HttpRequestMethodNotSupportedException("POST");
        var response = handler.handleMethodNotAllowed(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.success()).isFalse();
        assertThat(body.error().code()).isEqualTo(ErrorCode.METHOD_NOT_ALLOWED.name());
    }

    @Test
    @DisplayName("4백 번대 비즈니스 예외는 클라이언트 메시지를 그대로 반환한다")
    void handles4xxBusinessExceptionWithOriginalMessage() {
        BusinessException ex = new BusinessException(ErrorCode.REQUEST_VALIDATION_ERROR, "invalid round");

        var response = handler.handleBusiness(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().message()).isEqualTo("invalid round");
    }

    @Test
    @DisplayName("5백 번대 비즈니스 예외는 기본 메시지로 마스킹된다")
    void handles5xxBusinessExceptionWithDefaultMessage() {
        BusinessException ex = new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "internal details");

        var response = handler.handleBusiness(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().message())
                .isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage());
    }

    @Test
    @DisplayName("메서드 인자 타입 불일치 예외은 400으로 처리한다")
    void handlesTypeMismatchAs400() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);

        var response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo(ErrorCode.REQUEST_VALIDATION_ERROR.name());
    }

    @Test
    @DisplayName("제약 조건 위반 예외은 400으로 처리한다")
    void handlesConstraintViolationAs400() {
        ConstraintViolationException ex = new ConstraintViolationException(java.util.Set.of());

        var response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("리소스 없음 예외은 404로 처리한다")
    void handlesNoResourceFoundAs404() {
        NoResourceFoundException ex = mock(NoResourceFoundException.class);

        var response = handler.handleNoResource(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().error().code()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.name());
    }

    @Test
    @DisplayName("처리되지 않은 예외는 500으로 처리한다")
    void handlesUnexpectedExceptionAs500() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getMethod()).thenReturn("POST");
        when(req.getRequestURI()).thenReturn("/ops/collect");

        var response = handler.handleUnexpected(new RuntimeException("unexpected"), req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().error().code()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.name());
    }
}
