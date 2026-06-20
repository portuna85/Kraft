package com.kraft.winningnumber;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("LottoFreshnessMetrics 단위 테스트")
class LottoFreshnessMetricsTest {

    @Mock
    private WinningNumberRepository winningNumberRepository;

    @Mock
    private LottoDrawScheduleCalculator drawScheduleCalculator;

    @Test
    @DisplayName("최신 회차가 없으면 모든 freshness metric은 0이어야 한다")
    void snapshot_returnsZeroesWhenNoWinningNumberExists() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-20T12:00:00Z"), ZoneId.of("Asia/Seoul"));
        given(winningNumberRepository.findTopByOrderByRoundDesc()).willReturn(Optional.empty());

        LottoFreshnessMetrics metrics = new LottoFreshnessMetrics(
                new SimpleMeterRegistry(), winningNumberRepository, clock, drawScheduleCalculator);

        LottoFreshnessMetrics.FreshnessSnapshot snapshot = metrics.snapshot();
        assertThat(snapshot.latestRound()).isZero();
        assertThat(snapshot.expectedLatestRound()).isZero();
        assertThat(snapshot.staleDays()).isZero();
    }

    @Test
    @DisplayName("최신 회차가 있으면 최신/예상/지연 일수를 한 번의 snapshot으로 계산한다")
    void snapshot_returnsDerivedFreshnessValues() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-20T12:00:00Z"), ZoneId.of("Asia/Seoul"));
        WinningNumber latest = new WinningNumber(
                1200,
                LocalDate.of(2026, 6, 13),
                3, 11, 19, 28, 34, 42,
                7,
                2_000_000_000L,
                0L, 0, 0L, 0L,
                ZonedDateTime.now(clock).toOffsetDateTime()
        );
        given(winningNumberRepository.findTopByOrderByRoundDesc()).willReturn(Optional.of(latest));
        given(drawScheduleCalculator.expectedLatestDrawDate(ZonedDateTime.now(clock).withZoneSameInstant(ZoneId.of("Asia/Seoul"))))
                .willReturn(LocalDate.of(2026, 6, 20));

        LottoFreshnessMetrics metrics = new LottoFreshnessMetrics(
                new SimpleMeterRegistry(), winningNumberRepository, clock, drawScheduleCalculator);

        LottoFreshnessMetrics.FreshnessSnapshot snapshot = metrics.snapshot();
        assertThat(snapshot.latestRound()).isEqualTo(1200);
        assertThat(snapshot.expectedLatestRound()).isEqualTo(1201);
        assertThat(snapshot.staleDays()).isEqualTo(7);
    }
}
