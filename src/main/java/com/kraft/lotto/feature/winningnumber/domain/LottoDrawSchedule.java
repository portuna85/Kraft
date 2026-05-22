package com.kraft.lotto.feature.winningnumber.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/**
 * 로또 추첨 회차와 날짜의 관계를 계산하는 유틸리티.
 * 1회 추첨일: 2002-12-07 (토요일)
 */
public final class LottoDrawSchedule {

    public static final LocalDate FIRST_DRAW_DATE = LocalDate.of(2002, 12, 7);

    private LottoDrawSchedule() {}

    /**
     * 주어진 날짜 기준으로 가장 최근에 추첨된 예상 회차를 반환합니다.
     * 추첨은 매주 토요일이며, 토요일 당일은 해당 회차로 계산합니다.
     */
    public static int expectedRound(LocalDate asOf) {
        LocalDate lastSaturday = asOf.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY));
        if (lastSaturday.isBefore(FIRST_DRAW_DATE)) {
            return 0;
        }
        long weeks = ChronoUnit.WEEKS.between(FIRST_DRAW_DATE, lastSaturday);
        return (int) weeks + 1;
    }
}
