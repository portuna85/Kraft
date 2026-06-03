package com.kraft.lotto.feature.winningnumber.web.dto;

public record SumRangeStatDto(
        int rangeStart,
        int rangeEnd,
        long drawCount,
        double percent,
        long maxDrawCount,
        double theoreticalPercent
) {}
