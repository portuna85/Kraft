package com.kraft.lotto.infra.health;

import com.kraft.lotto.feature.winningnumber.domain.LottoDrawSchedule;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberEntity;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("winningNumberDb")
@SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "Spring-managed bean validates constructor dependencies")
public class LottoApiHealthIndicator implements HealthIndicator {

    private final WinningNumberRepository winningNumberRepository;
    private final Clock clock;
    private final MeterRegistry meterRegistry;

    @Autowired
    public LottoApiHealthIndicator(WinningNumberRepository winningNumberRepository,
                                   Clock clock,
                                   ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this.winningNumberRepository = winningNumberRepository;
        this.clock = clock;
        this.meterRegistry = meterRegistryProvider.getIfAvailable(SimpleMeterRegistry::new);
    }

    @Override
    @Transactional(readOnly = true)
    public Health health() {
        long startedAt = System.nanoTime();
        String status = "up";
        try {
            Optional<WinningNumberEntity> latest = winningNumberRepository.findTopByOrderByRoundDesc();
            int latestRound = latest.map(WinningNumberEntity::getRound).orElse(0);
            int expectedRound = LottoDrawSchedule.expectedRound(LocalDate.now(clock));
            LocalDateTime lastCollectedAt = latest.map(WinningNumberEntity::getFetchedAt).orElse(null);

            Health.Builder builder = Health.up()
                    .withDetail("latestStoredRound", latestRound)
                    .withDetail("expectedRound", expectedRound)
                    .withDetail("behindRounds", Math.max(0, expectedRound - latestRound));
            if (lastCollectedAt != null) {
                builder.withDetail("lastCollectedAt", lastCollectedAt);
            }
            return builder.build();
        } catch (RuntimeException ex) {
            status = "down";
            recordFailure();
            return Health.down().withDetail("error", "repository_query_failed").build();
        } finally {
            recordLatency(startedAt, status);
        }
    }

    private void recordLatency(long startedAt, String status) {
        meterRegistry.timer("kraft.health.winning-number-db.latency", "status", status)
                .record(Duration.ofNanos(System.nanoTime() - startedAt));
    }

    private void recordFailure() {
        meterRegistry.counter("kraft.health.winning-number-db.failure").increment();
    }
}
