package com.kraft.winningnumber;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WinningNumberFreshnessScheduler {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Logger log = LoggerFactory.getLogger(WinningNumberFreshnessScheduler.class);

    private final WinningNumberRepository winningNumberRepository;
    private final Clock clock;

    public WinningNumberFreshnessScheduler(WinningNumberRepository winningNumberRepository, Clock clock) {
        this.winningNumberRepository = winningNumberRepository;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    @SchedulerLock(name = "check-freshness", lockAtMostFor = "PT5M")
    public void warnWhenStale() {
        winningNumberRepository.findTopByOrderByRoundDesc().ifPresent(latest -> {
            LocalDate today = LocalDate.now(clock.withZone(KST));
            LocalDate expected = today.with(DayOfWeek.SATURDAY);
            // Saturday draw happens at ~20:35 KST; checking at 09:00 means today's draw
            // has not yet occurred. Use <= so Saturday morning also looks at last week's draw.
            // Sunday (value=7) naturally resolves with(SATURDAY) to yesterday within the same ISO week.
            if (today.getDayOfWeek().getValue() <= DayOfWeek.SATURDAY.getValue()) {
                expected = expected.minusWeeks(1);
            }
            if (latest.getDrawDate().isBefore(expected)) {
                log.warn("당첨번호 데이터가 최신이 아닙니다. latestRound={} drawDate={} expectedAtLeast={}",
                        latest.getRound(), latest.getDrawDate(), expected);
            }
        });
    }
}
