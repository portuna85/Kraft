package com.kraft.lotto.feature.recommend.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.kraft.lotto.feature.recommend.web.dto.RecommendStatsDto;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

@DisplayName("추천 메트릭 조회 서비스")
class RecommendMetricsQueryServiceTest {

    private static RecommendMetricsQueryService serviceWith(MeterRegistry registry) {
        ObjectProvider<MeterRegistry> provider = new ObjectProvider<>() {
            @Override
            public MeterRegistry getObject() { return registry; }

            @Override
            public MeterRegistry getIfAvailable(java.util.function.Supplier<MeterRegistry> defaultSupplier) {
                return registry;
            }
        };
        return new RecommendMetricsQueryService(provider);
    }

    @Test
    @DisplayName("메트릭이 하나도 없으면 모든 필드가 0/비어있다")
    void emptyRegistryReturnsZeroSnapshot() {
        RecommendMetricsQueryService service = serviceWith(new SimpleMeterRegistry());

        RecommendStatsDto dto = service.getSnapshot();

        assertThat(dto.generationCount()).isEqualTo(0L);
        assertThat(dto.generationMeanMs()).isEqualTo(0.0);
        assertThat(dto.generationMaxMs()).isEqualTo(0.0);
        assertThat(dto.requestedSetCount()).isEqualTo(0L);
        assertThat(dto.failuresByReason()).isEmpty();
    }

    @Test
    @DisplayName("latency timer가 기록된 경우 count/mean/max를 반환한다")
    void withLatencyTimerReturnsStats() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        registry.timer("kraft.recommend.generation.latency")
                .record(200, TimeUnit.MILLISECONDS);
        registry.timer("kraft.recommend.generation.latency")
                .record(400, TimeUnit.MILLISECONDS);

        RecommendMetricsQueryService service = serviceWith(registry);
        RecommendStatsDto dto = service.getSnapshot();

        assertThat(dto.generationCount()).isEqualTo(2L);
        assertThat(dto.generationMeanMs()).isGreaterThan(0.0);
        assertThat(dto.generationMaxMs()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("request.count summary가 기록된 경우 requestedSetCount를 반환한다")
    void withRequestSummaryReturnsCount() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        registry.summary("kraft.recommend.request.count").record(3);
        registry.summary("kraft.recommend.request.count").record(5);

        RecommendMetricsQueryService service = serviceWith(registry);
        RecommendStatsDto dto = service.getSnapshot();

        assertThat(dto.requestedSetCount()).isEqualTo(8L);
    }

    @Test
    @DisplayName("generation.failure 카운터가 기록된 경우 failuresByReason에 집계된다")
    void withFailureCountersReturnsByReason() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        registry.counter("kraft.recommend.generation.failure", "reason", "initial_pick_timeout").increment();
        registry.counter("kraft.recommend.generation.failure", "reason", "fixup_timeout").increment();
        registry.counter("kraft.recommend.generation.failure", "reason", "fixup_timeout").increment();

        RecommendMetricsQueryService service = serviceWith(registry);
        RecommendStatsDto dto = service.getSnapshot();

        assertThat(dto.failuresByReason()).containsEntry("initial_pick_timeout", 1L);
        assertThat(dto.failuresByReason()).containsEntry("fixup_timeout", 2L);
    }
}
