package com.kraft.lotto.support;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ModelAndView handleBusiness(BusinessException ex) {
        ErrorCode code = ex.getErrorCode();
        if (code.getHttpStatus().is5xxServerError()) {
            log.error("BusinessException: {} - {}", code.name(), LogSanitizer.sanitizeLogValue(ex.getMessage()), ex);
        } else {
            log.info("BusinessException: {} - {}", code.name(), LogSanitizer.sanitizeLogValue(ex.getMessage()));
        }
        String message = code.getHttpStatus().is5xxServerError()
                ? code.getDefaultMessage()
                : ex.getMessage();
        return errorView(code.getHttpStatus(), "요청 처리 실패", message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ModelAndView handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return errorView(HttpStatus.BAD_REQUEST, "잘못된 요청", "요청 파라미터 값을 확인해 주세요.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ModelAndView handleNoResource(NoResourceFoundException ex) {
        return errorView(HttpStatus.NOT_FOUND, "페이지를 찾을 수 없습니다", "요청하신 주소를 확인해 주세요.");
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleUnexpected(Exception ex, HttpServletRequest req) {
        String safeQuery = maskSensitiveQuery(req.getQueryString());
        String safeMethod = LogSanitizer.sanitizeLogValue(req.getMethod());
        String safePath = LogSanitizer.maskSensitivePath(req.getRequestURI());
        log.error("Unhandled exception at {} {} (query={})",
                safeMethod, safePath, safeQuery, ex);
        return errorView(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류", "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
    }

    private static ModelAndView errorView(HttpStatus status, String title, String message) {
        ModelAndView mav = new ModelAndView("error");
        mav.setStatus(status);
        mav.addObject("errorTitle", title);
        mav.addObject("errorMessage", message);
        return mav;
    }

    static String maskSensitiveQuery(String queryString) {
        return LogSanitizer.maskSensitiveQuery(queryString);
    }
}