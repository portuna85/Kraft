package com.kraft.lotto.feature.statistics.application;

import com.kraft.lotto.feature.statistics.infrastructure.CompanionPairSummaryRepository;
import com.kraft.lotto.feature.statistics.infrastructure.PatternStatsSummaryRepository;
import com.kraft.lotto.feature.statistics.infrastructure.WinningNumberFrequencySummaryRepository;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;

class WinningStatisticsCacheServiceBuilder {

    private final WinningNumberRepository repository;
    private WinningNumberFrequencySummaryRepository summaryRepository;
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();
    private Clock clock = Clock.systemDefaultZone();
    private PatternStatsSummaryRepository patternStatsSummaryRepository;
    private CompanionPairSummaryRepository companionPairSummaryRepository;

    private WinningStatisticsCacheServiceBuilder(WinningNumberRepository repository) {
        this.repository = repository;
    }

    static WinningStatisticsCacheServiceBuilder forRepository(WinningNumberRepository repository) {
        return new WinningStatisticsCacheServiceBuilder(repository);
    }

    WinningStatisticsCacheServiceBuilder summaryRepository(WinningNumberFrequencySummaryRepository repo) {
        this.summaryRepository = repo;
        return this;
    }

    WinningStatisticsCacheServiceBuilder meterRegistry(MeterRegistry registry) {
        this.meterRegistry = registry;
        return this;
    }

    WinningStatisticsCacheServiceBuilder clock(Clock clock) {
        this.clock = clock;
        return this;
    }

    WinningStatisticsCacheServiceBuilder patternStats(PatternStatsSummaryRepository repo) {
        this.patternStatsSummaryRepository = repo;
        return this;
    }

    WinningStatisticsCacheServiceBuilder companionPair(CompanionPairSummaryRepository repo) {
        this.companionPairSummaryRepository = repo;
        return this;
    }

    WinningStatisticsCacheService build() {
        return new WinningStatisticsCacheService(
                repository, summaryRepository, meterRegistry, clock,
                patternStatsSummaryRepository, companionPairSummaryRepository);
    }
}
