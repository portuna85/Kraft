package com.kraft.lotto.feature.winningnumber.application;

import com.kraft.lotto.feature.winningnumber.domain.WinningStore;
import com.kraft.lotto.feature.winningnumber.event.WinningNumbersCollectedEvent;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningStoreEntity;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningStoreRepository;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
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
        if (storeRepository.existsByRound(latestRound)) {
            log.debug("winning stores already collected: round={}", latestRound);
            return;
        }
        collectStores(latestRound);
    }

    public boolean collectStores(int round) {
        log.info("collecting winning stores: round={}", round);
        StoreFetchBatch batch = fetchAllGrades(round);
        return persist(batch);
    }

    private StoreFetchBatch fetchAllGrades(int round) {
        List<WinningStoreEntity> result = new java.util.ArrayList<>();
        boolean complete = true;
        for (int grade : GRADES) {
            List<WinningStore> stores = storeApiClient.fetchStores(round, grade);
            if (stores.isEmpty()) {
                log.warn("no winning stores fetched: round={}, grade={}", round, grade);
                complete = false;
                continue;
            }
            stores.stream()
                    .map(s -> new WinningStoreEntity(s.round(), s.grade(), s.name(), s.address(), s.winCount()))
                    .forEach(result::add);
            log.info("winning stores fetched: round={}, grade={}, count={}", round, grade, stores.size());
        }
        return new StoreFetchBatch(round, complete, List.copyOf(result));
    }

    @Transactional
    public boolean persist(StoreFetchBatch batch) {
        if (!batch.complete()) {
            log.warn("winning store fetch incomplete for round={}, skipping persist", batch.round());
            return false;
        }
        storeRepository.deleteByRound(batch.round());
        if (!batch.entities().isEmpty()) {
            storeRepository.saveAll(batch.entities());
            log.info("winning stores saved: round={}, count={}", batch.round(), batch.entities().size());
        }
        return true;
    }

    record StoreFetchBatch(int round, boolean complete, List<WinningStoreEntity> entities) {
    }
}
