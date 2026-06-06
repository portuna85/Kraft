package com.kraft.lotto.feature.winningnumber.application;

import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningStoreRepository;
import java.util.stream.IntStream;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression("${kraft.lotto.scheduler.enabled:true} and ${kraft.collect.auto.enabled:true}")
class WinningStoreAutoCollectScheduler {

    private static final Logger log = LoggerFactory.getLogger(WinningStoreAutoCollectScheduler.class);
    private static final String STORE_COLLECT_LOCK_NAME = "store-collect-auto";
    private static final int[] GRADES = {1, 2};

    private final WinningStoreCollector storeCollector;
    private final WinningNumberRepository winningNumberRepository;
    private final WinningStoreRepository storeRepository;

    WinningStoreAutoCollectScheduler(WinningStoreCollector storeCollector,
                                     WinningNumberRepository winningNumberRepository,
                                     WinningStoreRepository storeRepository) {
        this.storeCollector = storeCollector;
        this.winningNumberRepository = winningNumberRepository;
        this.storeRepository = storeRepository;
    }

    @Scheduled(
            cron = "${kraft.collect.auto.cron.store-sunday-09-00:0 0 9 ? * SUN}",
            zone = "${kraft.collect.auto.zone:Asia/Seoul}"
    )
    @SchedulerLock(name = STORE_COLLECT_LOCK_NAME, lockAtMostFor = "PT5M", lockAtLeastFor = "PT1M")
    public void collectStoreSunday0900() {
        runStoreCollect("store-sun-09-00");
    }

    private void runStoreCollect(String trigger) {
        int latestRound = winningNumberRepository.findMaxRound().orElse(0);
        if (latestRound <= 0) {
            log.debug("store auto-collect skip: no rounds in DB, trigger={}", trigger);
            return;
        }
        boolean alreadyComplete = IntStream.of(GRADES)
                .allMatch(g -> storeRepository.existsByRoundAndGrade(latestRound, g));
        if (alreadyComplete) {
            log.debug("store auto-collect skip: already complete, trigger={}, round={}", trigger, latestRound);
            return;
        }
        log.info("store auto-collect start: trigger={}, round={}", trigger, latestRound);
        boolean success = storeCollector.collectStores(latestRound);
        log.info("store auto-collect done: trigger={}, round={}, success={}", trigger, latestRound, success);
    }
}
