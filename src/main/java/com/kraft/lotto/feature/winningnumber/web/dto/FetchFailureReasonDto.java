package com.kraft.lotto.feature.winningnumber.web.dto;

public record FetchFailureReasonDto(
        String reason,
        long count
) {
}

