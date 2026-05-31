package com.kraft.lotto.feature.recommend.web.dto;

import java.util.Map;

public record RecommendStatsDto(
        long generationCount,
        double generationMeanMs,
        double generationMaxMs,
        long requestedSetCount,
        Map<String, Long> failuresByReason
) {
    public RecommendStatsDto {
        failuresByReason = Map.copyOf(failuresByReason);
    }
}
