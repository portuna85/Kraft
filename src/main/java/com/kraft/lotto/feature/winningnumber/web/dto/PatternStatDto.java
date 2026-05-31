package com.kraft.lotto.feature.winningnumber.web.dto;

import java.util.List;

public record PatternStatDto(
        List<OddEvenStatDto> oddEvenStats,
        List<SumRangeStatDto> sumRangeStats,
        long totalDraws
) {}
