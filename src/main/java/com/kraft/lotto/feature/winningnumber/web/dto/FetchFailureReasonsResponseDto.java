package com.kraft.lotto.feature.winningnumber.web.dto;

import java.time.LocalDateTime;
import java.util.List;

public record FetchFailureReasonsResponseDto(
        LocalDateTime generatedAt,
        int limit,
        String reason,
        Integer drwNoFrom,
        Integer drwNoTo,
        List<FetchFailureReasonDto> items
) {
    public FetchFailureReasonsResponseDto {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
