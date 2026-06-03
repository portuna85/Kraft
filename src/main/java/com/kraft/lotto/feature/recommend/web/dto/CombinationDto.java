package com.kraft.lotto.feature.recommend.web.dto;

import java.util.List;

public record CombinationDto(List<Integer> numbers, List<String> passedLabels) {
    public CombinationDto {
        numbers = List.copyOf(numbers);
        passedLabels = passedLabels == null ? List.of() : List.copyOf(passedLabels);
    }
}
