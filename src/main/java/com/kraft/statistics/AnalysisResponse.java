package com.kraft.statistics;

import java.util.List;

public record AnalysisResponse(
        List<Integer> numbers,
        int oddCount,
        int evenCount,
        int lowCount,
        int highCount,
        int sumOfNumbers,
        String sumBucket,
        int consecutivePairCount,
        List<RangeDistribution> rangeDistribution
) {
    public record RangeDistribution(String range, int count) {
    }
}
