package com.kraft.winningnumber;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 운영 데이터 신선도를 Prometheus gauge로 노출한다. 값은 항상 DB 최신 상태를 반영하되,
 * 동일 scrape 안에서 gauge 3개가 각각 조회하는 중복 쿼리를 막기 위해 짧은 TTL로만 결과를
 * 재사용한다(scrape 주기 15초 대비 무시할 수 있는 지연).
 */
@Component
public class LottoFreshnessMetrics {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Duration SNAPSHOT_TTL = Duration.ofSeconds(1);

    private final WinningNumberRepository winningNumberRepository;
    private final Clock clock;
    private final LottoDrawScheduleCalculator drawScheduleCalculator;
    private volatile FreshnessSnapshot cached = new FreshnessSnapshot(0d, 0d, 0d);
    private volatile Instant cachedAt = Instant.EPOCH;

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

    synchronized FreshnessSnapshot snapshot() {
        Instant now = Instant.now(clock);
        if (Duration.between(cachedAt, now).compareTo(SNAPSHOT_TTL) < 0) {
            return cached;
        }

        LocalDate expected = expectedDrawDate();
        Optional<WinningNumber> latest = winningNumberRepository.findTopByOrderByRoundDesc();
        FreshnessSnapshot snapshot;
        if (latest.isEmpty()) {
            snapshot = new FreshnessSnapshot(0d, 0d, 0d);
        } else {
            WinningNumber winningNumber = latest.orElseThrow();
            long weeksBehind = Math.max(0, ChronoUnit.WEEKS.between(winningNumber.getDrawDate(), expected));
            long staleDays = Math.max(0, ChronoUnit.DAYS.between(winningNumber.getDrawDate(), expected));
            snapshot = new FreshnessSnapshot(
                    winningNumber.getRound(),
                    winningNumber.getRound() + weeksBehind,
                    staleDays
            );
        }
        cached = snapshot;
        cachedAt = now;
        return snapshot;
    }

    private LocalDate expectedDrawDate() {
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(KST);
        return drawScheduleCalculator.expectedLatestDrawDate(now);
    }

    record FreshnessSnapshot(double latestRound, double expectedLatestRound, double staleDays) {
    }
}
