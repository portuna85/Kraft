package com.kraft.lotto.web;

import com.kraft.lotto.feature.winningnumber.application.LottoFetchLogQueryService;
import com.kraft.lotto.feature.winningnumber.web.dto.FetchFailureLogsResponseDto;
import com.kraft.lotto.feature.winningnumber.web.dto.FetchFailureOverviewDto;
import com.kraft.lotto.feature.winningnumber.web.dto.FetchFailureReasonsResponseDto;
import com.kraft.lotto.feature.winningnumber.web.dto.FetchLogRetentionStatusDto;
import com.kraft.lotto.infra.config.KraftCollectProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@OpsApi
@RestController
@RequestMapping("/ops/fetch-logs")
@Tag(name = "Ops", description = "Operational endpoints for collection and failure logs")
public class OpsFetchLogController {

    private final LottoFetchLogQueryService fetchLogQueryService;
    private final KraftCollectProperties collectProperties;

    public OpsFetchLogController(LottoFetchLogQueryService fetchLogQueryService,
                                 KraftCollectProperties collectProperties) {
        this.fetchLogQueryService = fetchLogQueryService;
        this.collectProperties = collectProperties;
    }

    @GetMapping("/failure-reasons")
    @Operation(summary = "Summarize recent collection failure reasons")
    public FetchFailureReasonsResponseDto summarizeFailureReasons(
            @RequestParam(defaultValue = "200") int limit,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) Integer drwNoFrom,
            @RequestParam(required = false) Integer drwNoTo
    ) {
        NormalizedQuery query = normalize(limit, reason, drwNoFrom, drwNoTo);
        return fetchLogQueryService.failureReasonsResponse(query.limit(), query.reason(), query.from(), query.to());
    }

    @GetMapping("/failures")
    @Operation(summary = "List recent collection failure logs")
    public FetchFailureLogsResponseDto recentFailures(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) Integer drwNoFrom,
            @RequestParam(required = false) Integer drwNoTo
    ) {
        NormalizedQuery query = normalize(limit, reason, drwNoFrom, drwNoTo);
        return fetchLogQueryService.failuresResponse(query.limit(), query.reason(), query.from(), query.to());
    }

    @GetMapping("/failure-overview")
    @Operation(summary = "Get combined failure overview")
    public FetchFailureOverviewDto failureOverview(
            @RequestParam(defaultValue = "200") int reasonLimit,
            @RequestParam(defaultValue = "100") int logLimit,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) Integer drwNoFrom,
            @RequestParam(required = false) Integer drwNoTo
    ) {
        NormalizedQuery reasonQuery = normalize(reasonLimit, reason, drwNoFrom, drwNoTo);
        int safeLogLimit = OpsQueryParams.normalizeLogLimit(logLimit);
        return fetchLogQueryService.failureOverview(
                reasonQuery.limit(), safeLogLimit, reasonQuery.reason(), reasonQuery.from(), reasonQuery.to());
    }

    @GetMapping("/retention-status")
    @Operation(summary = "Get fetch log retention configuration and purge eligibility")
    public FetchLogRetentionStatusDto fetchLogRetentionStatus() {
        return OpsRetentionStatusSupport.resolve(fetchLogQueryService, collectProperties);
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

    private record NormalizedQuery(int limit, String reason, Integer from, Integer to) {
    }
}
