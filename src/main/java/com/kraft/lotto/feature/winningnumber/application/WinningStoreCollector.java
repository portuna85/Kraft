package com.kraft.lotto.feature.winningnumber.application;

import com.kraft.lotto.feature.winningnumber.domain.WinningStore;
import com.kraft.lotto.feature.winningnumber.event.WinningNumbersCollectedEvent;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningStoreEntity;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningStoreRepository;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class WinningStoreCollector {

    private static final Logger log = LoggerFactory.getLogger(WinningStoreCollector.class);
    private static final int[] GRADES = {1, 2};

    private final WinningStoreApiClient storeApiClient;
    private final WinningStoreRepository storeRepository;
    private final WinningNumberRepository winningNumberRepository;
    private final Clock clock;

    @Async
    @EventListener
    public void onCollected(WinningNumbersCollectedEvent event) {
        if (!event.dataChanged()) {
            return;
        }
        int latestRound = winningNumberRepository.findMaxRound().orElse(0);
        if (latestRound <= 0) {
            return;
        }
        boolean allCollected = java.util.Arrays.stream(GRADES)
                .allMatch(g -> storeRepository.existsByRoundAndGrade(latestRound, g));
        if (allCollected) {
            log.debug("winning stores already fully collected: round={}", latestRound);
            return;
        }
        collectStores(latestRound);
    }

    public boolean collectStores(int round) {
        log.info("collecting winning stores: round={}", round);
        boolean allSuccess = true;
        for (int grade : GRADES) {
            List<WinningStore> stores = storeApiClient.fetchStores(round, grade);
            if (stores.isEmpty()) {
                log.warn("no winning stores fetched: round={}, grade={}", round, grade);
                allSuccess = false;
                continue;
            }
            persistGrade(round, grade, stores);
        }
        return allSuccess;
    }

    @Transactional
    void persistGrade(int round, int grade, List<WinningStore> stores) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<WinningStoreEntity> entities = stores.stream()
                .map(s -> new WinningStoreEntity(s.round(), s.grade(), s.name(), s.address(), s.winCount(), now))
                .toList();
        storeRepository.deleteByRoundAndGrade(round, grade);
        storeRepository.saveAll(entities);
        log.info("winning stores saved: round={}, grade={}, count={}", round, grade, entities.size());
    }
}
