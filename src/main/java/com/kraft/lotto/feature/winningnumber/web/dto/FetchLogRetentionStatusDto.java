package com.kraft.lotto.feature.winningnumber.web.dto;

import java.time.LocalDateTime;

public record FetchLogRetentionStatusDto(
        LocalDateTime generatedAt,
        boolean enabled,
        int retentionDays,
        int deleteBatchSize,
        String cron,
        String zone,
        LocalDateTime cutoff,
        long totalLogs,
        long purgeEligibleLogs,
        LocalDateTime oldestFetchedAt,
        LocalDateTime newestFetchedAt
) {
}

