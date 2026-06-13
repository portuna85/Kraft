package com.kraft.statistics;

import java.util.List;

public record CompanionStatsResponse(
        int totalRounds,
        List<CompanionPairDto> topPairs
) {
}
