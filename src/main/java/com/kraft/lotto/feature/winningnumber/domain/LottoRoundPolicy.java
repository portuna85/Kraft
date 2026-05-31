package com.kraft.lotto.feature.winningnumber.domain;

public final class LottoRoundPolicy {

    public static final int MIN_ROUND = 1;
    public static final int MAX_ROUND = 3000;
    public static final int MIN_BALL = 1;
    public static final int MAX_BALL = 45;

    private LottoRoundPolicy() {
    }

    public static int clamp(int round) {
        return Math.max(MIN_ROUND, Math.min(round, MAX_ROUND));
    }
}

