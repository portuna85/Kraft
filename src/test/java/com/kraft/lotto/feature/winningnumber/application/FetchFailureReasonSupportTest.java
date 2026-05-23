package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;

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
}
