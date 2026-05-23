package com.kraft.lotto.feature.recommend.web.dto;

import java.util.Map;

public record RecommendStatsDto(
        long generationCount,
        double generationMeanMs,
        double generationMaxMs,
        long timeoutCount,
        Map<String, Long> failuresByReason,
        long attemptCount,
        long rejectionCount,
        Map<String, Long> rejectionsByRule
) {
    public RecommendStatsDto {
        failuresByReason = Map.copyOf(failuresByReason);
        rejectionsByRule = Map.copyOf(rejectionsByRule);
    }
}
