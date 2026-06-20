package com.kraft.winningnumber;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 운영 데이터 신선도를 Prometheus gauge로 노출한다. 값은 스크레이프 시점에 매번 계산되므로
 * (캐시된 상태가 아님) 별도 스케줄 없이도 항상 최신 DB 상태를 반영한다.
 */
@Component
public class LottoFreshnessMetrics {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final WinningNumberRepository winningNumberRepository;
    private final Clock clock;
    private final LottoDrawScheduleCalculator drawScheduleCalculator;

    public LottoFreshnessMetrics(MeterRegistry registry,
                                  WinningNumberRepository winningNumberRepository,
                                  Clock clock,
                                  LottoDrawScheduleCalculator drawScheduleCalculator) {
        this.winningNumberRepository = winningNumberRepository;
        this.clock = clock;
        this.drawScheduleCalculator = drawScheduleCalculator;

        Gauge.builder("kraft_lotto_latest_round", this, LottoFreshnessMetrics::latestRound)
                .description("DB에 저장된 최신 로또 회차")
                .register(registry);
        Gauge.builder("kraft_lotto_expected_latest_round", this, LottoFreshnessMetrics::expectedLatestRound)
                .description("현재 시각 기준 존재해야 할 최소 회차")
                .register(registry);
        Gauge.builder("kraft_lotto_data_stale_days", this, LottoFreshnessMetrics::staleDays)
                .description("기대 추첨일 대비 최신 저장 데이터의 지연 일수")
                .register(registry);
    }

    private double latestRound() {
        return snapshot().latestRound();
    }

    private double expectedLatestRound() {
        return snapshot().expectedLatestRound();
    }

    private double staleDays() {
        return snapshot().staleDays();
    }

    FreshnessSnapshot snapshot() {
        LocalDate expected = expectedDrawDate();
        Optional<WinningNumber> latest = winningNumberRepository.findTopByOrderByRoundDesc();
        if (latest.isEmpty()) {
            return new FreshnessSnapshot(0d, 0d, 0d);
        }

        WinningNumber winningNumber = latest.orElseThrow();
        long weeksBehind = Math.max(0, ChronoUnit.WEEKS.between(winningNumber.getDrawDate(), expected));
        long staleDays = Math.max(0, ChronoUnit.DAYS.between(winningNumber.getDrawDate(), expected));
        return new FreshnessSnapshot(
                winningNumber.getRound(),
                winningNumber.getRound() + weeksBehind,
                staleDays
        );
    }

    private LocalDate expectedDrawDate() {
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(KST);
        return drawScheduleCalculator.expectedLatestDrawDate(now);
    }

    record FreshnessSnapshot(double latestRound, double expectedLatestRound, double staleDays) {
    }
}
