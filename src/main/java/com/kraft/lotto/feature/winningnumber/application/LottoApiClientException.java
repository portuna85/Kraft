package com.kraft.lotto.feature.winningnumber.application;

/**
 * 외부 로또 당첨번호 API 호출/파싱 단계의 시스템 예외.
 * 비즈니스 계층에서 {@code EXTERNAL_API_FAILURE}로 매핑된다.
 */
public class LottoApiClientException extends RuntimeException {

    public enum FailureReason {
        HTTP_ERROR("http_error"),
        BLANK_BODY("blank_body"),
        NON_JSON("non_json"),
        HTML_UPSTREAM_BLOCKED("html_upstream_blocked"),
        NETWORK("network"),
        TIMEOUT("timeout"),
        JSON_PARSE("json_parse"),
        VALIDATION("validation"),
        TRANSFORM("transform"),
        UNEXPECTED_RETURN_VALUE("unexpected_return_value"),
        CIRCUIT_OPEN("circuit_open"),
        MISSING_FIELD("missing_field"),
        OTHER("other");

        private final String metricName;

        FailureReason(String metricName) {
            this.metricName = metricName;
        }

        public String metricName() {
            return metricName;
        }
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
        return failureReason.metricName();
    }
}
