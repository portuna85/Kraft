package com.kraft.lotto.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@DisplayName("글로벌 예외 핸들러 테스트")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("BusinessException은 에러 뷰로 렌더링한다")
    void handlesBusinessExceptionAsErrorView() {
        var ex = new BusinessException(ErrorCode.LOTTO_INVALID_COUNT, "count 오류");
        ModelAndView mav = handler.handleBusiness(ex);
        assertThat(mav.getViewName()).isEqualTo("error");
        assertThat(mav.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(mav.getModel().get("errorMessage")).isEqualTo("count 오류");
    }

    @Test
    @DisplayName("타입 불일치 예외는 400 에러 뷰로 처리한다")
    void handlesTypeMismatchAsBadRequest() {
        var ex = new MethodArgumentTypeMismatchException("abc", Integer.class, "drwNo", null, new NumberFormatException());
        ModelAndView mav = handler.handleTypeMismatch(ex);
        assertThat(mav.getViewName()).isEqualTo("error");
        assertThat(mav.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("자원을 찾을 수 없는 경우 404 에러 뷰로 처리한다")
    void handlesNoResourceFoundAs404() {
        var ex = new NoResourceFoundException(HttpMethod.GET, "/missing", "");
        ModelAndView mav = handler.handleNoResource(ex);
        assertThat(mav.getViewName()).isEqualTo("error");
        assertThat(mav.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("허용되지 않은 HTTP 메서드는 405 에러 뷰로 처리한다")
    void handlesMethodNotAllowedAs405() {
        var ex = new HttpRequestMethodNotSupportedException("POST");
        ModelAndView mav = handler.handleMethodNotAllowed(ex);
        assertThat(mav.getViewName()).isEqualTo("error");
        assertThat(mav.getStatus()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(mav.getModel().get("errorMessage")).isEqualTo(ErrorCode.METHOD_NOT_ALLOWED.getDefaultMessage());
    }

    @Test
    @DisplayName("예상치 못한 예외는 500 에러 뷰로 처리하고 민감 쿼리를 마스킹한다")
    void handlesUnexpectedAs500() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/");
        req.setQueryString("token=secret123&count=5");
        ModelAndView mav = handler.handleUnexpected(new RuntimeException("boom"), req);
        assertThat(mav.getViewName()).isEqualTo("error");
        assertThat(mav.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("민감 쿼리 파라미터를 마스킹한다")
    void masksSensitiveQueryParameters() {
        String masked = GlobalExceptionHandler.maskSensitiveQuery("token=abc&count=5&password=pw&foo=bar");
        assertThat(masked).contains("token=***").contains("password=***").contains("count=5").contains("foo=bar");
    }

    @Test
    @DisplayName("로그를 위해 민감한 경로 세그먼트와 줄바꿈을 정화한다")
    void sanitizesPathSegmentsAndLineBreaks() {
        String maskedPath = LogSanitizer.maskSensitivePath("/api/token/abc123/detail\r\nx");
        String maskedQuery = GlobalExceptionHandler.maskSensitiveQuery("token=abc\r\nnext=1");

        assertThat(maskedPath).contains("/token/***").doesNotContain("abc123").doesNotContain("\r").doesNotContain("\n");
        assertThat(maskedQuery).contains("token=***").doesNotContain("\r").doesNotContain("\n");
    }

    @Test
    @DisplayName("5xx BusinessException은 내부 메시지 대신 defaultMessage를 반환한다")
    void hidesInternalMessageFor5xxBusinessException() {
        var ex = new BusinessException(ErrorCode.COLLECT_FAILED, "내부 상세 메시지");
        ModelAndView mav = handler.handleBusiness(ex);
        assertThat(mav.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(mav.getModel().get("errorMessage")).isEqualTo(ErrorCode.COLLECT_FAILED.getDefaultMessage());
        assertThat((String) mav.getModel().get("errorMessage")).doesNotContain("내부 상세 메시지");
    }
}
