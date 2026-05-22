package com.kraft.lotto.feature.winningnumber.web.dto;

import java.util.List;

public record CombinationPrizeHistoryDto(
        List<Integer> numbers,
        int firstPrizeCount,
        int secondPrizeCount,
        List<CombinationPrizeHitDto> firstPrizeHits,
        List<CombinationPrizeHitDto> secondPrizeHits
) {
    public CombinationPrizeHistoryDto {
        numbers = numbers == null ? List.of() : List.copyOf(numbers);
        firstPrizeHits = firstPrizeHits == null ? List.of() : List.copyOf(firstPrizeHits);
        secondPrizeHits = secondPrizeHits == null ? List.of() : List.copyOf(secondPrizeHits);
    }
}
