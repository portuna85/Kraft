package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("LottoDataFreshnessMetrics")
class LottoDataFreshnessMetricsTest {

    @Mock
    WinningNumberRepository winningNumberRepository;

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-06-07T12:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Test
    @DisplayName("게이지 kraft.latest_round.stored 는 DB 최신 회차를 반환한다")
    void storedRoundGaugeReturnsDbValue() {
        when(winningNumberRepository.findMaxRound()).thenReturn(Optional.of(1230));
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        new LottoDataFreshnessMetrics(winningNumberRepository, CLOCK, registry);

        assertThat(registry.get("kraft.latest_round.stored").gauge().value()).isEqualTo(1230.0);
    }

    @Test
    @DisplayName("게이지 kraft.latest_round.expected 는 날짜 기반 예상 회차를 반환한다")
    void expectedRoundGaugeReturnsComputedValue() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        new LottoDataFreshnessMetrics(winningNumberRepository, CLOCK, registry);

        double value = registry.get("kraft.latest_round.expected").gauge().value();
        assertThat(value).isEqualTo(1227.0);
    }
}
