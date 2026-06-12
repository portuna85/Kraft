package com.kraft.lotto.feature.recommend.application;

import com.kraft.lotto.support.BusinessException;
import com.kraft.lotto.support.ErrorCode;

import java.util.List;
import java.util.Set;

public record RecommendFilter(Integer oddCount, Integer sumMin, Integer sumMax, Set<String> disabledRules) {

    private static final int MIN_ODD_COUNT = 0;
    private static final int MAX_ODD_COUNT = 6;
    private static final int MIN_SUM = 21;
    private static final int MAX_SUM = 255;

    // oddCount별 달성 가능한 합산 범위 [minSum, maxSum] — 1~45에서 6개 선택
    // 홀수: 1,3,5,...,45 (23개) / 짝수: 2,4,...,44 (22개)
    private static final int[][] ODD_COUNT_SUM_BOUNDS = {
        {42, 234}, // oddCount=0: 짝수 6개
        {31, 235}, // oddCount=1: 홀수 1 + 짝수 5
        {24, 244}, // oddCount=2: 홀수 2 + 짝수 4
        {21, 249}, // oddCount=3: 홀수 3 + 짝수 3
        {22, 250}, // oddCount=4: 홀수 4 + 짝수 2
        {27, 249}, // oddCount=5: 홀수 5 + 짝수 1
        {36, 240}, // oddCount=6: 홀수 6개
    };

    public static final RecommendFilter NONE = new RecommendFilter(null, null, null, Set.of());

    public RecommendFilter {
        validateRange("oddCount", oddCount, MIN_ODD_COUNT, MAX_ODD_COUNT);
        validateRange("sumMin", sumMin, MIN_SUM, MAX_SUM);
        validateRange("sumMax", sumMax, MIN_SUM, MAX_SUM);
        if (sumMin != null && sumMax != null && sumMin > sumMax) {
            throw new BusinessException(ErrorCode.LOTTO_INVALID_NUMBER,
                    "sumMin must be less than or equal to sumMax");
        }
        if (oddCount != null && (sumMin != null || sumMax != null)) {
            int[] bounds = ODD_COUNT_SUM_BOUNDS[oddCount];
            int effectiveMin = sumMin != null ? sumMin : MIN_SUM;
            int effectiveMax = sumMax != null ? sumMax : MAX_SUM;
            if (effectiveMax < bounds[0] || effectiveMin > bounds[1]) {
                throw new BusinessException(ErrorCode.LOTTO_INVALID_NUMBER,
                        "oddCount=" + oddCount + "일 때 달성 가능한 합산 범위는 "
                        + bounds[0] + "~" + bounds[1] + "이며, 요청한 범위(" + effectiveMin + "~" + effectiveMax + ")와 겹치지 않습니다.");
            }
        }
        disabledRules = disabledRules == null ? Set.of() : Set.copyOf(disabledRules);
    }

    private static void validateRange(String name, Integer value, int min, int max) {
        if (value != null && (value < min || value > max)) {
            throw new BusinessException(ErrorCode.LOTTO_INVALID_NUMBER,
                    name + " must be between " + min + " and " + max);
        }
    }

    public boolean hasOddCount() {
        return oddCount != null;
    }

    public boolean hasSumRange() {
        return sumMin != null || sumMax != null;
    }

    public boolean isRuleDisabled(String ruleName) {
        return disabledRules.contains(ruleName);
    }

    public boolean isNone() {
        return !hasOddCount() && !hasSumRange() && disabledRules.isEmpty();
    }

    public static RecommendFilter of(Integer oddCount, Integer sumMin, Integer sumMax) {
        return new RecommendFilter(oddCount, sumMin, sumMax, Set.of());
    }

    public static RecommendFilter of(Integer oddCount, Integer sumMin, Integer sumMax,
                                     List<String> disabledRules) {
        Set<String> disabled = (disabledRules == null || disabledRules.isEmpty())
                ? Set.of()
                : Set.copyOf(disabledRules);
        return new RecommendFilter(oddCount, sumMin, sumMax, disabled);
    }
}
