package com.kraft.lotto.feature.winningnumber.application;

/**
 * 외부 로또 당첨번호 API 호출/파싱 단계의 시스템 예외.
 * 비즈니스 계층에서 {@code EXTERNAL_API_FAILURE}로 매핑된다.
 */
public class LottoApiClientException extends RuntimeException {

    public enum FailureReason {
        HTTP_ERROR,
        BLANK_BODY,
        NON_JSON,
        NETWORK,
        TIMEOUT,
        JSON_PARSE,
        VALIDATION,
        TRANSFORM,
        UNEXPECTED_RETURN_VALUE,
        CIRCUIT_OPEN,
        MISSING_FIELD,
        OTHER
    }

    private final Integer responseCode;
    private final String rawResponse;
    private final FailureReason failureReason;

    public LottoApiClientException(String message) {
        this(message, null, null, null, FailureReason.OTHER);
    }

    public LottoApiClientException(String message, Throwable cause) {
        this(message, cause, null, null, FailureReason.OTHER);
    }

    public LottoApiClientException(String message, FailureReason failureReason) {
        this(message, null, null, null, failureReason);
    }

    public LottoApiClientException(String message, Throwable cause, FailureReason failureReason) {
        this(message, cause, null, null, failureReason);
    }

    public LottoApiClientException(String message, Integer responseCode, String rawResponse) {
        this(message, null, responseCode, rawResponse, FailureReason.OTHER);
    }

    public LottoApiClientException(String message, Integer responseCode, String rawResponse, FailureReason failureReason) {
        this(message, null, responseCode, rawResponse, failureReason);
    }

    public LottoApiClientException(String message, Throwable cause, Integer responseCode, String rawResponse) {
        this(message, cause, responseCode, rawResponse, FailureReason.OTHER);
    }

    public LottoApiClientException(String message, Throwable cause, Integer responseCode, String rawResponse, FailureReason failureReason) {
        super(message, cause);
        this.responseCode = responseCode;
        this.rawResponse = rawResponse;
        this.failureReason = failureReason == null ? FailureReason.OTHER : failureReason;
    }

    public Integer getResponseCode() {
        return responseCode;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public FailureReason getFailureReason() {
        return failureReason;
    }

    public String metricReason() {
        return switch (failureReason) {
            case HTTP_ERROR -> "http_error";
            case BLANK_BODY -> "blank_body";
            case NON_JSON -> "non_json";
            case NETWORK -> "network";
            case TIMEOUT -> "timeout";
            case JSON_PARSE -> "json_parse";
            case VALIDATION -> "validation";
            case TRANSFORM -> "transform";
            case UNEXPECTED_RETURN_VALUE -> "unexpected_return_value";
            case CIRCUIT_OPEN -> "circuit_open";
            case MISSING_FIELD -> "missing_field";
            case OTHER -> "other";
        };
    }
}
