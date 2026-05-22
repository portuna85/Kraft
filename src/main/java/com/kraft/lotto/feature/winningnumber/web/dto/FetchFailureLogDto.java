package com.kraft.lotto.feature.winningnumber.web.dto;

import java.time.LocalDateTime;

public record FetchFailureLogDto(
        long id,
        int drwNo,
        Integer responseCode,
        String reason,
        String message,
        LocalDateTime fetchedAt
) {
}

