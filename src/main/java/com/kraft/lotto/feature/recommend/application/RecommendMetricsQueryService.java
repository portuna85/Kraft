package com.kraft.lotto.feature.recommend.application;

import com.kraft.lotto.feature.recommend.web.dto.RecommendStatsDto;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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

        DistributionSummary requestSummary = meterRegistry.find("kraft.recommend.request.count").summary();
        long requestedSetCount = requestSummary == null ? 0L : (long) requestSummary.totalAmount();

        Map<String, Long> failuresByReason = new TreeMap<>();
        meterRegistry.find("kraft.recommend.generation.failure").counters()
                .forEach(c -> {
                    Counter counter = (Counter) c;
                    failuresByReason.put(counter.getId().getTag("reason"), (long) counter.count());
                });

        return new RecommendStatsDto(
                generationCount, generationMeanMs, generationMaxMs,
                requestedSetCount, failuresByReason
        );
    }
}
