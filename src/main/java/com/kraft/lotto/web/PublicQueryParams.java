package com.kraft.lotto.web;

import com.kraft.lotto.feature.winningnumber.domain.LottoRoundPolicy;

final class PublicQueryParams {

    private static final int MIN_COUNT = 1;
    private static final int MAX_COUNT = 10;
    private static final int MIN_PAGE = 0;
    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 100;

    private PublicQueryParams() {
    }

    static int normalizeCount(int count) {
        return Math.max(MIN_COUNT, Math.min(count, MAX_COUNT));
    }

    static Integer normalizeRound(Integer round) {
        if (round == null) {
            return null;
        }
        return LottoRoundPolicy.clamp(round);
    }

    static int normalizePage(int page) {
        return Math.max(MIN_PAGE, page);
    }

    static int normalizeSize(int size) {
        return Math.max(MIN_SIZE, Math.min(size, MAX_SIZE));
    }
}
