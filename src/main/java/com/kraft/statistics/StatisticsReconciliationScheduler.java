package com.kraft.statistics;

import com.kraft.winningnumber.WinningNumber;
import com.kraft.winningnumber.WinningNumberRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 통계 summary는 원래 {@link StatisticsRefreshListener}(수집/보정 이벤트 리스너)로만 갱신되므로,
 * 이벤트 발행이 어떤 이유로든 유실되면 최대 다음 재조정 시점까지 옛 회차에 머문다. 이 스케줄러가
 * 매주 자동 수집 직후 프로젝션이 뒤처졌는지 확인해 필요할 때만 전체 재계산으로 따라잡는다.
 */
@Component
public class StatisticsReconciliationScheduler {

    private static final Logger log = LoggerFactory.getLogger(StatisticsReconciliationScheduler.class);

    private final FrequencySummaryRepository frequencySummaryRepository;
    private final WinningNumberRepository winningNumberRepository;
    private final StatisticsSummaryRebuilder summaryRebuilder;

    public StatisticsReconciliationScheduler(FrequencySummaryRepository frequencySummaryRepository,
                                             WinningNumberRepository winningNumberRepository,
                                             StatisticsSummaryRebuilder summaryRebuilder) {
        this.frequencySummaryRepository = frequencySummaryRepository;
        this.winningNumberRepository = winningNumberRepository;
        this.summaryRebuilder = summaryRebuilder;
    }

    // 자동 수집(일 07:00) 직후 여유를 두어 그 수집이 먼저 끝나도록 한다.
    @Scheduled(cron = "0 30 7 * * SUN", zone = "Asia/Seoul")
    @SchedulerLock(name = "statistics-reconcile", lockAtMostFor = "PT15M", lockAtLeastFor = "PT1M")
    public void reconcileIfBehind() {
        int projected = frequencySummaryRepository.findMaxLastRound();
        int latest = winningNumberRepository.findTopByOrderByRoundDesc()
                .map(WinningNumber::getRound)
                .orElse(0);
        if (latest > projected) {
            log.info("통계 summary 지연 감지 — 재조정 시작: projected={} latest={}", projected, latest);
            summaryRebuilder.rebuildAllSummaries();
        }
    }
}
