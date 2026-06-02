package com.kraft.lotto.web;

import com.kraft.lotto.feature.recommend.application.RecommendMetricsQueryService;
import com.kraft.lotto.feature.recommend.web.dto.RecommendStatsDto;
import com.kraft.lotto.feature.winningnumber.application.ApiCircuitBreakerRegistry;
import com.kraft.lotto.feature.winningnumber.web.dto.OpsCircuitBreakerStatusDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@OpsApi
@RestController
@RequestMapping("/ops")
@Tag(name = "Ops", description = "Operational endpoints for collection and failure logs")
public class OpsMonitoringController {

    private final RecommendMetricsQueryService recommendMetricsQueryService;
    private final ApiCircuitBreakerRegistry circuitBreakerRegistry;
    private final Clock clock;

    public OpsMonitoringController(RecommendMetricsQueryService recommendMetricsQueryService,
                                   ApiCircuitBreakerRegistry circuitBreakerRegistry,
                                   Clock clock) {
        this.recommendMetricsQueryService = recommendMetricsQueryService;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.clock = clock;
    }

    @GetMapping("/recommend/stats")
    @Operation(summary = "Get recommendation generation metrics snapshot")
    public RecommendStatsDto recommendStats() {
        return recommendMetricsQueryService.getSnapshot();
    }

    @GetMapping("/circuit-breakers")
    @Operation(summary = "Get external API circuit breaker states")
    public OpsCircuitBreakerStatusDto circuitBreakers() {
        return new OpsCircuitBreakerStatusDto(LocalDateTime.now(clock), mapCircuitBreakerStates());
    }

    private Map<String, OpsCircuitBreakerStatusDto.CircuitBreakerState> mapCircuitBreakerStates() {
        return circuitBreakerRegistry.snapshots().entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> new OpsCircuitBreakerStatusDto.CircuitBreakerState(
                                entry.getValue().enabled(),
                                entry.getValue().state()
                        ),
                        (a, b) -> a,
                        TreeMap::new
                ));
    }
}
