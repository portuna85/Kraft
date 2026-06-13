package com.kraft.recommend;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;

public record RecommendNumbersRequest(
        @Min(1) @Max(5) Integer count,
        List<Integer> excludedNumbers
) {
}
