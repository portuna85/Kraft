package com.kraft.lotto.feature.winningnumber.web.dto;

import java.time.LocalDateTime;
import java.util.List;

public record FetchFailureOverviewDto(
        LocalDateTime generatedAt,
        int reasonLimit,
        int logLimit,
        List<FetchFailureReasonDto> reasons,
        List<FetchFailureLogDto> recentFailures
) {
    public FetchFailureOverviewDto {
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
        recentFailures = recentFailures == null ? List.of() : List.copyOf(recentFailures);
    }
}
