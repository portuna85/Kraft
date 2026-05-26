package com.kraft.lotto.support;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    LOTTO_INVALID_NUMBER(HttpStatus.BAD_REQUEST, "유효하지 않은 로또 번호입니다.", false),
    LOTTO_INVALID_TARGET_ROUND(HttpStatus.BAD_REQUEST, "targetRound는 1 이상이어야 합니다.", false),
    REQUEST_VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "요청 값이 유효하지 않습니다.", false),
    LOTTO_GENERATION_TIMEOUT(HttpStatus.SERVICE_UNAVAILABLE, "추천 조합 생성 시도를 초과했습니다.", true),
    WINNING_NUMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "요청하신 회차의 당첨번호를 찾을 수 없습니다.", false),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청하신 리소스를 찾을 수 없습니다.", false),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 HTTP 메서드입니다.", false),
    EXTERNAL_API_FAILURE(HttpStatus.BAD_GATEWAY, "외부 API 호출에 실패했습니다.", true),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "내부 서버 오류가 발생했습니다.", true);

    private final HttpStatus httpStatus;
    private final String defaultMessage;
    private final boolean retryable;

    ErrorCode(HttpStatus httpStatus, String defaultMessage, boolean retryable) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
        this.retryable = retryable;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
