package com.kraft.lotto.feature.winningnumber.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.kraft.lotto.feature.winningnumber.infrastructure.LottoFetchStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("데이터 변경 로그 DTO")
class DataChangeLogDtoTest {

    @Test
    @DisplayName("fetchedAt 포맷이 yyyy-MM-dd HH:mm 형식이다")
    void formatsDate() {
        DataChangeLogDto dto = DataChangeLogDto.of(1000, LottoFetchStatus.SUCCESS,
                LocalDateTime.of(2026, 6, 1, 22, 30));
        assertThat(dto.fetchedAtFormatted()).isEqualTo("2026-06-01 22:30");
    }

    @Test
    @DisplayName("fetchedAt이 null이면 빈 문자열이다")
    void nullDateReturnsEmpty() {
        DataChangeLogDto dto = DataChangeLogDto.of(1000, LottoFetchStatus.SUCCESS, null);
        assertThat(dto.fetchedAtFormatted()).isEmpty();
    }

    @ParameterizedTest(name = "{0} → 레이블={1}, CSS={2}")
    @CsvSource({
        "SUCCESS,   수집 성공, changelog-success",
        "FAILED,    수집 실패, changelog-failed",
        "SKIPPED,   스킵,     changelog-skipped",
        "NOT_DRAWN, 미추첨,   changelog-not-drawn"
    })
    @DisplayName("상태별 레이블과 CSS 클래스가 올바르다")
    void statusLabelAndCss(LottoFetchStatus status, String label, String css) {
        DataChangeLogDto dto = DataChangeLogDto.of(1, status, null);
        assertThat(dto.statusLabel()).isEqualTo(label.trim());
        assertThat(dto.statusCssClass()).isEqualTo(css.trim());
        assertThat(dto.status()).isEqualTo(status);
    }
}
