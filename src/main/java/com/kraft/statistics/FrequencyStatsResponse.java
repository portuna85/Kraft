package com.kraft.statistics;

import java.util.List;

public record FrequencyStatsResponse(
        int totalRounds,
        List<BallFrequencyDto> frequencies
) {
}
