package com.kraft.winningnumber;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
    private final LottoDrawScheduleCalculator drawScheduleCalculator;

    public WinningNumberFreshnessScheduler(WinningNumberRepository winningNumberRepository,
                                           Clock clock,
                                           LottoDrawScheduleCalculator drawScheduleCalculator) {
        this.winningNumberRepository = winningNumberRepository;
        this.clock = clock;
        this.drawScheduleCalculator = drawScheduleCalculator;
    }

    @Scheduled(cron = "0 0 7 * * SUN", zone = "Asia/Seoul")
    @SchedulerLock(name = "check-freshness", lockAtMostFor = "PT5M")
    public void warnWhenStale() {
        winningNumberRepository.findTopByOrderByRoundDesc().ifPresent(latest -> {
            ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(KST);
            LocalDate expected = drawScheduleCalculator.expectedLatestDrawDate(now);
            if (latest.getDrawDate().isBefore(expected)) {
                log.warn("당첨번호 데이터가 최신이 아닙니다. latestRound={} drawDate={} expectedAtLeast={}",
                        latest.getRound(), latest.getDrawDate(), expected);
            }
        });
    }
}
