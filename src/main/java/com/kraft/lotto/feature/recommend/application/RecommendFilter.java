package com.kraft.lotto.feature.recommend.application;

import com.kraft.lotto.support.BusinessException;
import com.kraft.lotto.support.ErrorCode;

import java.util.List;
import java.util.Set;

public record RecommendFilter(Integer oddCount, Integer sumMin, Integer sumMax, Set<String> disabledRules) {

    public static final RecommendFilter NONE = new RecommendFilter(null, null, null, Set.of());
    private static final int MIN_ODD_COUNT = 0;
    private static final int MAX_ODD_COUNT = 6;
    private static final int MIN_SUM = 21;
    private static final int MAX_SUM = 255;

    public RecommendFilter {
        validateRange("oddCount", oddCount, MIN_ODD_COUNT, MAX_ODD_COUNT);
        validateRange("sumMin", sumMin, MIN_SUM, MAX_SUM);
        validateRange("sumMax", sumMax, MIN_SUM, MAX_SUM);
        if (sumMin != null && sumMax != null && sumMin > sumMax) {
            throw new BusinessException(ErrorCode.LOTTO_INVALID_NUMBER,
                    "sumMin must be less than or equal to sumMax");
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
