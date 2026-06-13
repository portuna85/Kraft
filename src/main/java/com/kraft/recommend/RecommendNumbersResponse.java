package com.kraft.recommend;

import java.util.List;

public record RecommendNumbersResponse(
        List<List<Integer>> recommendations
) {
}
