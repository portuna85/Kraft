package com.kraft.lotto.feature.winningnumber.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class LottoRoundPolicy {

    public static final int MIN_ROUND = 1;
    /** Bean validation @Max requires a compile-time constant. Use maxCollectableRound() for runtime checks. */
    public static final int MAX_ROUND = 9999;
    public static final int MIN_BALL = 1;
    public static final int MAX_BALL = 45;

    private static final LocalDate FIRST_DRAW_DATE = LocalDate.of(2002, 12, 7);
    private static final int HEADROOM = 10;

    private LottoRoundPolicy() {
    }

    /** 수집 허용 상한: HEADROOM 포함. 내부 수집 검증에만 사용한다. */
    public static int maxCollectableRound(LocalDate today) {
        long weeks = ChronoUnit.WEEKS.between(FIRST_DRAW_DATE, today);
        return (int) Math.max(1, weeks + 1) + HEADROOM;
    }

    /** 사용자 조회 상한: HEADROOM 없이 현재 날짜 기준 계산 회차. UI 표시용. */
    public static int maxUserSearchRound(LocalDate today) {
        long weeks = ChronoUnit.WEEKS.between(FIRST_DRAW_DATE, today);
        return (int) Math.max(1, weeks + 1);
    }

    public static int clamp(int round) {
        return Math.max(MIN_ROUND, Math.min(round, maxCollectableRound(LocalDate.now())));
    }
}

