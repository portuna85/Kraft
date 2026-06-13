package com.kraft.statistics;

import com.kraft.winningnumber.WinningNumbersCollectedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class StatisticsRefreshListener {

    private static final Logger log = LoggerFactory.getLogger(StatisticsRefreshListener.class);

    private final WinningStatisticsCacheService statisticsCacheService;

    public StatisticsRefreshListener(WinningStatisticsCacheService statisticsCacheService) {
        this.statisticsCacheService = statisticsCacheService;
    }

    @Async
    @EventListener
    public void onCollected(WinningNumbersCollectedEvent event) {
        if (!event.dataChanged()) {
            return;
        }
        log.info("WinningNumbersCollectedEvent 수신 — statistics summary 갱신: round={}", event.round());
        try {
            statisticsCacheService.rebuildAllSummaries();
        } catch (Exception e) {
            log.warn("statistics summary 갱신 실패 (무시): {}", e.getMessage());
        }
    }
}
