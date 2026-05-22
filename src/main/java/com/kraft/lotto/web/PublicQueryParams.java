package com.kraft.lotto.web;

final class PublicQueryParams {

    private static final int MIN_COUNT = 1;
    private static final int MAX_COUNT = 10;
    private static final int MIN_ROUND = 1;
    private static final int MAX_ROUND = 3000;
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
        return Math.max(MIN_ROUND, Math.min(round, MAX_ROUND));
    }

    static int normalizePage(int page) {
        return Math.max(MIN_PAGE, page);
    }

    static int normalizeSize(int size) {
        return Math.max(MIN_SIZE, Math.min(size, MAX_SIZE));
    }
}
