package com.kraft.lotto.feature.winningnumber.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class LottoRoundPolicy {

    public static final int MIN_ROUND = 1;
    /** Bean validation @Max requires a compile-time constant. Use maxPossibleRound() for runtime checks. */
    public static final int MAX_ROUND = 9999;
    public static final int MIN_BALL = 1;
    public static final int MAX_BALL = 45;

    private static final LocalDate FIRST_DRAW_DATE = LocalDate.of(2002, 12, 7);
    private static final int HEADROOM = 10;

    private LottoRoundPolicy() {
    }

    /** 공식 첫 회차(2002-12-07) 기준으로 오늘까지 가능한 최대 회차를 계산한다. */
    public static int maxPossibleRound(LocalDate today) {
        long weeks = ChronoUnit.WEEKS.between(FIRST_DRAW_DATE, today);
        return (int) Math.max(1, weeks + 1) + HEADROOM;
    }

    public static int clamp(int round) {
        return Math.max(MIN_ROUND, Math.min(round, maxPossibleRound(LocalDate.now())));
    }
}

