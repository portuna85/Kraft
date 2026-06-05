package com.kraft.lotto.feature.winningnumber.application;

import com.kraft.lotto.feature.winningnumber.domain.LottoRoundPolicy;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class LottoDataFreshnessMetrics {

    private final WinningNumberRepository winningNumberRepository;
    private final Clock clock;

    public LottoDataFreshnessMetrics(WinningNumberRepository winningNumberRepository,
                                     Clock clock,
                                     MeterRegistry meterRegistry) {
        this.winningNumberRepository = winningNumberRepository;
        this.clock = clock;
        Gauge.builder("kraft.latest_round.stored", this, LottoDataFreshnessMetrics::storedRound)
                .description("DB에 저장된 최신 로또 회차")
                .register(meterRegistry);
        Gauge.builder("kraft.latest_round.expected", this, LottoDataFreshnessMetrics::expectedRound)
                .description("현재 날짜 기준 예상 최신 회차")
                .register(meterRegistry);
    }

    private double storedRound() {
        return winningNumberRepository.findMaxRound().orElse(0);
    }

    private double expectedRound() {
        return LottoRoundPolicy.maxPossibleRound(LocalDate.now(clock));
    }
}
