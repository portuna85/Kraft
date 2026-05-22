package com.kraft.lotto.feature.winningnumber.web.dto;

import java.time.LocalDateTime;
import java.util.List;

public record FetchFailureLogsResponseDto(
        LocalDateTime generatedAt,
        int limit,
        String reason,
        Integer drwNoFrom,
        Integer drwNoTo,
        List<FetchFailureLogDto> items
) {
    public FetchFailureLogsResponseDto {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
