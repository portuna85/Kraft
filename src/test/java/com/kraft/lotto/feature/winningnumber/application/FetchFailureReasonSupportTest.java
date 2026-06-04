package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.SocketTimeoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("수집 실패 사유 지원 테스트")
class FetchFailureReasonSupportTest {

    @Test
    @DisplayName("LottoApiClientException reason을 reason prefix로 정규화한다")
    void normalizesReasonFromException() {
        LottoApiClientException ex = new LottoApiClientException(
                "response parse failed (round=1200)",
                LottoApiClientException.FailureReason.JSON_PARSE
        );

        String normalized = FetchFailureReasonSupport.normalizeFailureMessage(ex);

        assertThat(normalized).startsWith("reason=json_parse;");
        assertThat(FetchFailureReasonSupport.extractReason(normalized)).isEqualTo("json_parse");
    }

    @Test
    @DisplayName("이미 reason prefix가 있는 문자열은 그대로 유지한다")
    void keepsExistingReasonPrefix() {
        String message = "reason=timeout; external API timeout exceeded";

        String normalized = FetchFailureReasonSupport.normalizeFailureMessage(message);

        assertThat(normalized).isEqualTo(message);
        assertThat(FetchFailureReasonSupport.extractReason(normalized)).isEqualTo("timeout");
        assertThat(FetchFailureReasonSupport.stripReasonPrefix(normalized))
                .isEqualTo("external API timeout exceeded");
    }

    @Test
    @DisplayName("알 수 없는 일반 예외 메시지는 other로 분류한다")
    void classifiesUnknownMessageAsOther() {
        String normalized = FetchFailureReasonSupport.normalizeFailureMessage("some unknown failure");

        assertThat(FetchFailureReasonSupport.extractReason(normalized)).isEqualTo("other");
    }

    @Test
    @DisplayName("SocketTimeoutException 원인은 timeout으로 우선 분류한다")
    void classifiesSocketTimeoutCauseAsTimeout() {
        LottoApiClientException ex = new LottoApiClientException(
                "some other message",
                new SocketTimeoutException("read timed out"),
                LottoApiClientException.FailureReason.OTHER
        );

        String normalized = FetchFailureReasonSupport.normalizeFailureMessage(ex);

        assertThat(FetchFailureReasonSupport.extractReason(normalized)).isEqualTo("timeout");
    }

    @Test
    @DisplayName("VALIDATION FailureReason이 있는 예외는 validation으로 정규화한다")
    void normalizesValidationReasonFromException() {
        LottoApiClientException ex = new LottoApiClientException(
                "round mismatch: request=100, response=101",
                LottoApiClientException.FailureReason.VALIDATION
        );

        String normalized = FetchFailureReasonSupport.normalizeFailureMessage(ex);

        assertThat(FetchFailureReasonSupport.extractReason(normalized)).isEqualTo("validation");
    }

    @Test
    @DisplayName("MISSING_FIELD FailureReason이 있는 예외는 missing_field로 정규화한다")
    void normalizesMissingFieldReasonFromException() {
        LottoApiClientException ex = new LottoApiClientException(
                "missing required field: numbers",
                LottoApiClientException.FailureReason.MISSING_FIELD
        );

        String normalized = FetchFailureReasonSupport.normalizeFailureMessage(ex);

        assertThat(FetchFailureReasonSupport.extractReason(normalized)).isEqualTo("missing_field");
    }

    @Test
    @DisplayName("FailureReason.OTHER일 때는 메시지 기반으로 분류한다")
    void fallsBackToMessageClassificationWhenReasonIsOther() {
        LottoApiClientException ex = new LottoApiClientException(
                "invalid date field: date",
                LottoApiClientException.FailureReason.OTHER
        );

        String normalized = FetchFailureReasonSupport.normalizeFailureMessage(ex);

        assertThat(FetchFailureReasonSupport.extractReason(normalized)).isEqualTo("validation");
    }
}
