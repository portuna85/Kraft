package com.kraft.lotto.feature.recommend.application;

import com.kraft.lotto.support.BusinessException;
import com.kraft.lotto.support.ErrorCode;

public record RecommendFilter(Integer oddCount, Integer sumMin, Integer sumMax) {

    public static final RecommendFilter NONE = new RecommendFilter(null, null, null);
    private static final int MIN_ODD_COUNT = 0;
    private static final int MAX_ODD_COUNT = 6;
    private static final int MIN_SUM = 21;
    private static final int MAX_SUM = 255;

    public RecommendFilter {
        if (oddCount != null && (oddCount < MIN_ODD_COUNT || oddCount > MAX_ODD_COUNT)) {
            throw new BusinessException(ErrorCode.LOTTO_INVALID_NUMBER,
                    "oddCount must be between " + MIN_ODD_COUNT + " and " + MAX_ODD_COUNT);
        }
        if (sumMin != null && (sumMin < MIN_SUM || sumMin > MAX_SUM)) {
            throw new BusinessException(ErrorCode.LOTTO_INVALID_NUMBER,
                    "sumMin must be between " + MIN_SUM + " and " + MAX_SUM);
        }
        if (sumMax != null && (sumMax < MIN_SUM || sumMax > MAX_SUM)) {
            throw new BusinessException(ErrorCode.LOTTO_INVALID_NUMBER,
                    "sumMax must be between " + MIN_SUM + " and " + MAX_SUM);
        }
        if (sumMin != null && sumMax != null && sumMin > sumMax) {
            throw new BusinessException(ErrorCode.LOTTO_INVALID_NUMBER,
                    "sumMin must be less than or equal to sumMax");
        }
    }

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
