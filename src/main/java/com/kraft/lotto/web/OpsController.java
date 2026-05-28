package com.kraft.lotto.web;

import com.kraft.lotto.feature.recommend.application.RecommendMetricsQueryService;
import com.kraft.lotto.feature.winningnumber.application.ApiCircuitBreakerRegistry;
import com.kraft.lotto.feature.recommend.web.dto.RecommendStatsDto;
import com.kraft.lotto.feature.winningnumber.application.LottoCollectionCommandService;
import com.kraft.lotto.feature.winningnumber.application.LottoFetchLogQueryService;
import com.kraft.lotto.feature.winningnumber.web.dto.CollectResponse;
import com.kraft.lotto.feature.winningnumber.web.dto.CollectStatusResponse;
import com.kraft.lotto.feature.winningnumber.web.dto.FetchFailureLogsResponseDto;
import com.kraft.lotto.feature.winningnumber.web.dto.FetchFailureOverviewDto;
import com.kraft.lotto.feature.winningnumber.web.dto.FetchFailureReasonsResponseDto;
import com.kraft.lotto.feature.winningnumber.web.dto.FetchLogRetentionStatusDto;
import com.kraft.lotto.feature.winningnumber.web.dto.OpsCircuitBreakerStatusDto;
import com.kraft.lotto.infra.config.KraftCollectProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Ops", description = "Operational endpoints for collection and failure logs")
public class OpsController {

    private final LottoFetchLogQueryService fetchLogQueryService;
    private final LottoCollectionCommandService collectionCommandService;
    private final OpsCollectionFacade opsCollectionFacade;
    private final RecommendMetricsQueryService recommendMetricsQueryService;
    private final ApiCircuitBreakerRegistry circuitBreakerRegistry;
    private final boolean logRetentionEnabled;
    private final int logRetentionDays;
    private final int logRetentionDeleteBatchSize;
    private final String logRetentionCron;
    private final String collectZone;
    private final Clock clock;

    @Autowired
    public OpsController(LottoFetchLogQueryService fetchLogQueryService,
                         LottoCollectionCommandService collectionCommandService,
                         OpsCollectionFacade opsCollectionFacade,
                         RecommendMetricsQueryService recommendMetricsQueryService,
                         ApiCircuitBreakerRegistry circuitBreakerRegistry,
                         KraftCollectProperties collectProperties,
                         Clock clock) {
        this.fetchLogQueryService = fetchLogQueryService;
        this.collectionCommandService = collectionCommandService;
        this.opsCollectionFacade = opsCollectionFacade;
        this.recommendMetricsQueryService = recommendMetricsQueryService;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.logRetentionEnabled = collectProperties.logRetention().enabled();
        this.logRetentionDays = collectProperties.logRetention().days();
        this.logRetentionDeleteBatchSize = collectProperties.logRetention().deleteBatchSize();
        this.logRetentionCron = collectProperties.logRetention().cron();
        this.collectZone = collectProperties.auto().zone();
        this.clock = clock;
    }

    private record NormalizedQuery(int limit, String reason, Integer from, Integer to) {
    }

    @GetMapping("/ops/fetch-logs/failure-reasons")
    @Operation(summary = "Summarize recent collection failure reasons")
    public FetchFailureReasonsResponseDto summarizeFailureReasons(
            @RequestParam(defaultValue = "200") int limit,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) Integer drwNoFrom,
            @RequestParam(required = false) Integer drwNoTo,
            HttpServletResponse response
    ) {
        applyNoStore(response);
        NormalizedQuery query = normalize(limit, reason, drwNoFrom, drwNoTo);
        return fetchLogQueryService.failureReasonsResponse(query.limit(), query.reason(), query.from(), query.to());
    }

    @GetMapping("/ops/fetch-logs/failures")
    @Operation(summary = "List recent collection failure logs")
    public FetchFailureLogsResponseDto recentFailures(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) Integer drwNoFrom,
            @RequestParam(required = false) Integer drwNoTo,
            HttpServletResponse response
    ) {
        applyNoStore(response);
        NormalizedQuery query = normalize(limit, reason, drwNoFrom, drwNoTo);
        return fetchLogQueryService.failuresResponse(query.limit(), query.reason(), query.from(), query.to());
    }

    @GetMapping("/ops/fetch-logs/failure-overview")
    @Operation(summary = "Get combined failure overview")
    public FetchFailureOverviewDto failureOverview(
            @RequestParam(defaultValue = "200") int reasonLimit,
            @RequestParam(defaultValue = "100") int logLimit,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) Integer drwNoFrom,
            @RequestParam(required = false) Integer drwNoTo,
            HttpServletResponse response
    ) {
        applyNoStore(response);
        NormalizedQuery reasonQuery = normalize(reasonLimit, reason, drwNoFrom, drwNoTo);
        int safeLogLimit = OpsQueryParams.normalizeLogLimit(logLimit);
        return fetchLogQueryService.failureOverview(
                reasonQuery.limit(), safeLogLimit, reasonQuery.reason(), reasonQuery.from(), reasonQuery.to());
    }

    @GetMapping("/ops/fetch-logs/retention-status")
    @Operation(summary = "Get fetch log retention configuration and purge eligibility")
    public FetchLogRetentionStatusDto fetchLogRetentionStatus(HttpServletResponse response) {
        applyNoStore(response);
        return fetchLogQueryService.retentionStatus(
                logRetentionEnabled,
                logRetentionDays,
                logRetentionDeleteBatchSize,
                logRetentionCron,
                collectZone
        );
    }

    @GetMapping("/ops/collect/status")
    @Operation(summary = "Get current collection job status")
    public CollectStatusResponse collectStatus(HttpServletResponse response) {
        applyNoStore(response);
        return collectionCommandService.getStatus();
    }

    @PostMapping("/ops/collect")
    @Operation(summary = "Collect winning numbers up to latest round")
    public CollectResponse collectLatest(HttpServletResponse response) {
        applyNoStore(response);
        return opsCollectionFacade.collectLatest();
    }

    @PostMapping("/ops/collect/missing")
    @Operation(summary = "Collect only missing rounds once")
    public CollectResponse collectMissing(HttpServletResponse response) {
        applyNoStore(response);
        return opsCollectionFacade.collectMissing();
    }

    @GetMapping("/ops/recommend/stats")
    @Operation(summary = "Get recommendation generation metrics snapshot")
    public RecommendStatsDto recommendStats(HttpServletResponse response) {
        applyNoStore(response);
        return recommendMetricsQueryService.getSnapshot();
    }

    @GetMapping("/ops/circuit-breakers")
    @Operation(summary = "Get external API circuit breaker states")
    public OpsCircuitBreakerStatusDto circuitBreakers(HttpServletResponse response) {
        applyNoStore(response);
        Map<String, OpsCircuitBreakerStatusDto.CircuitBreakerState> clients = circuitBreakerRegistry.snapshots().entrySet()
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> new OpsCircuitBreakerStatusDto.CircuitBreakerState(
                                entry.getValue().enabled(),
                                entry.getValue().state()
                        ),
                        (a, b) -> a,
                        java.util.TreeMap::new
                ));
        return new OpsCircuitBreakerStatusDto(LocalDateTime.now(clock), clients);
    }

    private void applyNoStore(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }

    private static NormalizedQuery normalize(int limit, String reason, Integer drwNoFrom, Integer drwNoTo) {
        OpsQueryParams.Range range = OpsQueryParams.normalizeRange(drwNoFrom, drwNoTo);
        return new NormalizedQuery(
                OpsQueryParams.normalizeLimit(limit),
                OpsQueryParams.normalizeReason(reason),
                range.from(),
                range.to()
        );
    }
}
