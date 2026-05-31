package com.kraft.lotto.feature.winningnumber.web.dto;

public record OddEvenStatDto(
        int oddCount,
        int evenCount,
        long drawCount,
        double percent,
        long maxDrawCount
) {}
