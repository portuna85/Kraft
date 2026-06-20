package com.kraft.winningnumber;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

/**
 * "지금 시점에 최소한 존재해야 하는 회차"의 추첨일을 계산한다.
 *
 * <p>로또 추첨은 매주 토요일 20:35경 발표되며, 자동 수집 스케줄은 21:30부터 시도한다.
 * 과거 {@code WinningNumberFreshnessScheduler}(토요일 포함)와 {@code OpsService}(토요일 제외)가
 * 서로 다른 경계 조건을 사용해 토요일 하루 동안 신선도 판정이 어긋났다 — 이 컴포넌트로 통일한다.</p>
 */
@Component
public class LottoDrawScheduleCalculator {

    private static final DayOfWeek DRAW_DAY = DayOfWeek.SATURDAY;
    private static final LocalTime DRAW_CUTOFF = LocalTime.of(21, 30);

    public LocalDate expectedLatestDrawDate(ZonedDateTime nowKst) {
        LocalDate today = nowKst.toLocalDate();
        LocalDate saturday = today.with(DRAW_DAY);
        boolean beforeThisWeeksDraw = today.isBefore(saturday)
                || (today.isEqual(saturday) && nowKst.toLocalTime().isBefore(DRAW_CUTOFF));
        return beforeThisWeeksDraw ? saturday.minusWeeks(1) : saturday;
    }
}
