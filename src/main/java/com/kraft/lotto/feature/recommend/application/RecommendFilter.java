package com.kraft.lotto.feature.recommend.application;

public record RecommendFilter(Integer oddCount, Integer sumMin, Integer sumMax) {

    public static final RecommendFilter NONE = new RecommendFilter(null, null, null);

    public boolean hasOddCount() {
        return oddCount != null;
    }

    public boolean hasSumRange() {
        return sumMin != null || sumMax != null;
    }

    public boolean isNone() {
        return !hasOddCount() && !hasSumRange();
    }

    public static RecommendFilter of(Integer oddCount, Integer sumMin, Integer sumMax) {
        return new RecommendFilter(oddCount, sumMin, sumMax);
    }
}
