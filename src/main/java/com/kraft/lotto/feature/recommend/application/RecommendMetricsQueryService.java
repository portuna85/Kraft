package com.kraft.lotto.feature.recommend.application;

import com.kraft.lotto.feature.recommend.web.dto.RecommendStatsDto;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RecommendMetricsQueryService {

    private final MeterRegistry meterRegistry;

    @Autowired
    @SuppressFBWarnings(
            value = "CT_CONSTRUCTOR_THROW",
            justification = "Spring ObjectProvider lookup may throw; failing fast during bean construction is intentional."
    )
    public RecommendMetricsQueryService(ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this.meterRegistry = meterRegistryProvider.getIfAvailable(SimpleMeterRegistry::new);
    }

    public RecommendStatsDto getSnapshot() {
        Timer latencyTimer = meterRegistry.find("kraft.recommend.generation.latency").timer();
        long generationCount = latencyTimer == null ? 0L : latencyTimer.count();
        double generationMeanMs = latencyTimer == null ? 0.0 : latencyTimer.mean(TimeUnit.MILLISECONDS);
        double generationMaxMs = latencyTimer == null ? 0.0 : latencyTimer.max(TimeUnit.MILLISECONDS);

        Counter timeoutCounter = meterRegistry.find("kraft.recommend.timeout.count").counter();
        long timeoutCount = timeoutCounter == null ? 0L : (long) timeoutCounter.count();

        Counter attemptCounter = meterRegistry.find("kraft.recommend.attempt.count").counter();
        long attemptCount = attemptCounter == null ? 0L : (long) attemptCounter.count();

        Counter rejectionCounter = meterRegistry.find("kraft.recommend.rejection.count").counter();
        long rejectionCount = rejectionCounter == null ? 0L : (long) rejectionCounter.count();

        Map<String, Long> failuresByReason = new TreeMap<>();
        meterRegistry.find("kraft.recommend.generation.failure").counters()
                .forEach(c -> failuresByReason.put(c.getId().getTag("reason"), (long) c.count()));

        Map<String, Long> rejectionsByRule = new TreeMap<>();
        meterRegistry.find("kraft.recommend.rejection.by.rule").counters()
                .forEach(c -> rejectionsByRule.put(c.getId().getTag("rule"), (long) c.count()));

        return new RecommendStatsDto(
                generationCount, generationMeanMs, generationMaxMs,
                timeoutCount, failuresByReason,
                attemptCount, rejectionCount, rejectionsByRule
        );
    }

    private static RecommendStatsDto empty() {
        return new RecommendStatsDto(0, 0.0, 0.0, 0, Map.of(), 0, 0, Map.of());
    }
}
