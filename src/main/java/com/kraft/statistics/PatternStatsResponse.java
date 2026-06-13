package com.kraft.statistics;

import java.util.List;

public record PatternStatsResponse(
        int totalRounds,
        List<PatternBucketDto> oddCounts,
        List<PatternBucketDto> highCounts,
        List<PatternBucketDto> sumBuckets
) {
}
