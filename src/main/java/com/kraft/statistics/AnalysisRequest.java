package com.kraft.statistics;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AnalysisRequest(
        @NotNull @Size(min = 6, max = 6, message = "번호는 정확히 6개여야 합니다.")
        List<Integer> numbers
) {
}
