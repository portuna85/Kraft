package com.kraft.lotto.web;

import com.kraft.lotto.feature.recommend.web.dto.RecommendStatsDto;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import com.kraft.lotto.feature.winningnumber.application.LottoCollectionCommandService;
import com.kraft.lotto.feature.winningnumber.application.LottoFetchLogQueryService;
import com.kraft.lotto.feature.winningnumber.web.dto.CollectResponse;
import com.kraft.lotto.feature.winningnumber.web.dto.FetchFailureLogsResponseDto;
import com.kraft.lotto.feature.winningnumber.web.dto.FetchFailureOverviewDto;
import com.kraft.lotto.feature.winningnumber.web.dto.FetchFailureReasonsResponseDto;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Ops", description = "Operational endpoints for collection and failure logs")
@SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "Spring-managed constructor; ObjectProvider lookup does not throw")
public class OpsController {

    private final LottoFetchLogQueryService fetchLogQueryService;
    private final LottoCollectionCommandService collectionCommandService;
    private final MeterRegistry meterRegistry;

    @Autowired
    public OpsController(LottoFetchLogQueryService fetchLogQueryService,
                         LottoCollectionCommandService collectionCommandService,
                         ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this.fetchLogQueryService = fetchLogQueryService;
        this.collectionCommandService = collectionCommandService;
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
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
        int safeLogLimit = OpsQueryParams.normalizeLimit(logLimit);
        return fetchLogQueryService.failureOverview(
                reasonQuery.limit(), safeLogLimit, reasonQuery.reason(), reasonQuery.from(), reasonQuery.to());
    }

    @PostMapping("/ops/collect")
    @Operation(summary = "Collect winning numbers up to latest round")
    public CollectResponse collectLatest(HttpServletResponse response) {
        applyNoStore(response);
        return collectionCommandService.collectAllUntilLatest();
    }

    @PostMapping("/ops/collect/missing")
    @Operation(summary = "Collect only missing rounds once")
    public CollectResponse collectMissing(HttpServletResponse response) {
        applyNoStore(response);
        return collectionCommandService.collectMissingOnce();
    }

    @GetMapping("/ops/recommend/stats")
    @Operation(summary = "Get recommendation generation metrics snapshot")
    public RecommendStatsDto recommendStats(HttpServletResponse response) {
        applyNoStore(response);
        if (meterRegistry == null) {
            return new RecommendStatsDto(0, 0.0, 0.0, 0, Map.of(), 0, 0, Map.of());
        }
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
