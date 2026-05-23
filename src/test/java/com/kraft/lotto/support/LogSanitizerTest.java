package com.kraft.lotto.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("로그 정화기")
class LogSanitizerTest {

    @Test
    @DisplayName("null 값은 빈 문자열로 변환된다")
    void sanitizeNullReturnsEmpty() {
        assertThat(LogSanitizer.sanitizeLogValue(null)).isEmpty();
    }

    @Test
    @DisplayName("공백 값은 빈 문자열로 변환된다")
    void sanitizeBlankReturnsEmpty() {
        assertThat(LogSanitizer.sanitizeLogValue("   ")).isEmpty();
    }

    @Test
    @DisplayName("개행 문자를 언더스코어로 치환한다")
    void sanitizeReplacesNewlines() {
        assertThat(LogSanitizer.sanitizeLogValue("line1\nline2\rend"))
                .isEqualTo("line1_line2_end");
    }

    @Test
    @DisplayName("쿼리 파라미터에서 민감 키의 값을 마스킹한다")
    void maskSensitiveQueryMasksTokenValue() {
        assertThat(LogSanitizer.maskSensitiveQuery("page=1&token=secret123&sort=asc"))
                .isEqualTo("page=1&token=***&sort=asc");
    }

    @Test
    @DisplayName("null 쿼리 파라미터는 빈 문자열을 반환한다")
    void maskSensitiveQueryWithNull() {
        assertThat(LogSanitizer.maskSensitiveQuery(null)).isEmpty();
    }

    @Test
    @DisplayName("경로에서 민감 세그먼트를 마스킹한다")
    void maskSensitivePathMasksSensitiveSegment() {
        assertThat(LogSanitizer.maskSensitivePath("/ops/token/abc123"))
                .isEqualTo("/ops/token/***");
    }
}
