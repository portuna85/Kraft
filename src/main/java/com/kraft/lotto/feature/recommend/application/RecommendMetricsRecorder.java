package com.kraft.lotto.feature.recommend.application;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RecommendMetricsRecorder {

    private final MeterRegistry meterRegistry;

    @Autowired
    public RecommendMetricsRecorder(ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this(meterRegistryProvider.getIfAvailable(SimpleMeterRegistry::new));
    }

    RecommendMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordLatency(long startedAtNanos) {
        meterRegistry.timer("kraft.recommend.generation.latency")
                .record(System.nanoTime() - startedAtNanos, TimeUnit.NANOSECONDS);
    }

    public void recordRequestedCount(int count) {
        meterRegistry.summary("kraft.recommend.request.count").record(count);
    }

    public void recordFailure(RecommendGenerationTimeoutException ex) {
        String reason = switch (ex.getReason()) {
            case ATTEMPT_EXHAUSTED -> "attempt_exhausted";
            case INITIAL_PICK_TIMEOUT -> "initial_pick_timeout";
            case FIXUP_TIMEOUT -> "fixup_timeout";
            case OTHER -> "other";
        };
        meterRegistry.counter("kraft.recommend.generation.failure", "reason", reason).increment();
        meterRegistry.counter("kraft.recommend.generator.timeout.total", "phase", reason).increment();
    }
}
