package com.kraft.statistics;

import com.kraft.winningnumber.WinningNumbersCollectedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
public class StatisticsRefreshListener {

    private static final Logger log = LoggerFactory.getLogger(StatisticsRefreshListener.class);

    private final StatisticsSummaryRebuilder summaryRebuilder;

    public StatisticsRefreshListener(StatisticsSummaryRebuilder summaryRebuilder) {
        this.summaryRebuilder = summaryRebuilder;
    }

    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCollected(WinningNumbersCollectedEvent event) {
        if (!event.dataChanged()) {
            return;
        }
        log.info("WinningNumbersCollectedEvent 수신 — statistics summary 갱신: round={}", event.round());
        try {
            summaryRebuilder.rebuildAllSummaries();
        } catch (Exception e) {
            log.warn("statistics summary 갱신 실패 (무시): {}", e.getMessage());
        }
    }
}
