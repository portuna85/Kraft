package com.kraft.lotto.feature.winningnumber.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("로또 추첨 일정")
class LottoDrawScheduleTest {

    @Test
    @DisplayName("2026-06-07 기준 추첨 완료 예상 회차는 1227회다")
    void expectedRoundOn20260607Is1227() {
        assertThat(LottoDrawSchedule.expectedRound(LocalDate.of(2026, 6, 7))).isEqualTo(1227);
    }

    @Test
    @DisplayName("검색 허용 상한 여유분과 추첨 완료 회차는 분리된다")
    void expectedRoundDoesNotIncludeSearchHeadroom() {
        LocalDate asOf = LocalDate.of(2026, 6, 7);

        assertThat(LottoRoundPolicy.maxCollectableRound(asOf))
                .isGreaterThan(LottoDrawSchedule.expectedRound(asOf));
    }
}
