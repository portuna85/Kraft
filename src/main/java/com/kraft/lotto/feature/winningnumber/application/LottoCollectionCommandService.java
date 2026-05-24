package com.kraft.lotto.feature.winningnumber.application;

import com.kraft.lotto.feature.winningnumber.event.WinningNumbersCollectedEvent;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import com.kraft.lotto.feature.winningnumber.web.dto.CollectResponse;
import com.kraft.lotto.feature.winningnumber.web.dto.CollectStatusResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class LottoCollectionCommandService {

    private static final Logger log = LoggerFactory.getLogger(LottoCollectionCommandService.class);

    private final WinningNumberRepository winningNumberRepository;
    private final LottoSingleDrawCollector singleDrawCollector;
    private final LottoRangeCollector rangeCollector;
    private final ApplicationEventPublisher eventPublisher;
    private final BackfillDelaySupport backfillDelay;
    private final int maxCollectPerRun;
    private final int maxHistoryCollect;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<String> currentOperation = new AtomicReference<>(null);
    private final AtomicReference<Instant> startedAt = new AtomicReference<>(null);

    LottoCollectionCommandService(WinningNumberRepository winningNumberRepository,
                                  LottoSingleDrawCollector singleDrawCollector,
                                  LottoRangeCollector rangeCollector,
                                  ApplicationEventPublisher eventPublisher,
                                  long backfillDelayMs,
                                  int maxCollectPerRun,
                                  int maxHistoryCollect) {
        this.winningNumberRepository = winningNumberRepository;
        this.singleDrawCollector = singleDrawCollector;
        this.rangeCollector = rangeCollector;
        this.eventPublisher = eventPublisher;
        this.backfillDelay = new BackfillDelaySupport(backfillDelayMs);
        this.maxCollectPerRun = maxCollectPerRun;
        this.maxHistoryCollect = maxHistoryCollect;
    }

    public CollectResponse collectNextIfNeeded() {
        return runExclusive("collect-next", () -> {
            int nextRound = winningNumberRepository.findMaxRound().orElse(0) + 1;
            CollectResponse response = singleDrawCollector.collectOne(nextRound, false);
            publishCollected(response);
            return response;
        });
    }

    public CollectResponse collectAllUntilLatest() {
        return runExclusive("collect-all", () -> {
            int totalCollected = 0, totalUpdated = 0, totalSkipped = 0;
            List<Integer> allFailedRounds = new ArrayList<>();
            int latestRound = winningNumberRepository.findMaxRound().orElse(0);
            int nextRound = latestRound + 1;
            boolean truncated = true;

            for (int i = 0; i < maxCollectPerRun; i++) {
                CollectResponse one = singleDrawCollector.collectOne(nextRound, false);
                totalCollected += one.collected();
                totalUpdated += one.updated();
                totalSkipped += one.skipped();
                latestRound = Math.max(latestRound, one.latestRound());
                nextRound = latestRound + 1;
                allFailedRounds.addAll(one.failedRounds());
                if (one.notDrawn() || one.failed() > 0) {
                    truncated = false;
                    break;
                }
            }
            if (truncated) {
                log.warn("collect-all: MAX_COLLECT_PER_RUN({}) reached, collection stopped", maxCollectPerRun);
            }
            CollectResponse aggregated = CollectResponse.of(totalCollected, totalUpdated, totalSkipped,
                    latestRound, allFailedRounds, truncated, truncated ? latestRound + 1 : null, false);
            publishCollected(aggregated);
            return aggregated;
        });
    }

    public CollectResponse collectAllHistory() {
        return runExclusive("collect-history", () -> {
            int totalCollected = 0, totalUpdated = 0, totalSkipped = 0;
            List<Integer> allFailedRounds = new ArrayList<>();
            int latestRound = winningNumberRepository.findMaxRound().orElse(0);
            int nextRound = latestRound + 1;

            for (int i = 0; i < maxHistoryCollect; i++) {
                CollectResponse one = singleDrawCollector.collectOne(nextRound, false);
                totalCollected += one.collected();
                totalUpdated += one.updated();
                totalSkipped += one.skipped();
                latestRound = Math.max(latestRound, one.latestRound());
                nextRound = latestRound + 1;
                allFailedRounds.addAll(one.failedRounds());
                if (one.notDrawn() || one.failed() > 0) {
                    break;
                }
                if ((one.collected() > 0 || one.updated() > 0) && !backfillDelay.pauseIfPossible()) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            CollectResponse aggregated = CollectResponse.of(totalCollected, totalUpdated, totalSkipped,
                    latestRound, allFailedRounds, false, null, false);
            publishCollected(aggregated);
            return aggregated;
        });
    }

    public CollectResponse collectMissingOnce() {
        return runExclusive("collect-missing", () -> {
            int maxRound = winningNumberRepository.findMaxRound().orElse(0);
            if (maxRound <= 0) {
                return CollectResponse.of(0, 0, 0, 0, List.of(), false, null, false);
            }
            Set<Integer> existingRounds = winningNumberRepository.findRoundsBetween(1, maxRound);
            List<Integer> missingRounds = new ArrayList<>();
            for (int round = 1; round <= maxRound; round++) {
                if (!existingRounds.contains(round)) {
                    missingRounds.add(round);
                }
            }
            CollectResponse aggregated = rangeCollector.collectRange(missingRounds, false, true);
            publishCollected(aggregated);
            return aggregated;
        });
    }

    private void publishCollected(CollectResponse response) {
        if (eventPublisher == null) {
            return;
        }
        eventPublisher.publishEvent(WinningNumbersCollectedEvent.of(
                response.collected(), response.updated(), response.skipped(), response.failed()
        ));
    }

    public CollectStatusResponse getStatus() {
        if (!running.get()) {
            return CollectStatusResponse.idle();
        }
        return CollectStatusResponse.active(currentOperation.get(), startedAt.get());
    }

    private CollectResponse runExclusive(String operation, Supplier<CollectResponse> action) {
        if (!running.compareAndSet(false, true)) {
            int latestRound = winningNumberRepository.findMaxRound().orElse(0);
            log.warn("{} skipped: another collection run is already active", operation);
            return CollectResponse.ofOverlapSkipped(latestRound);
        }
        currentOperation.set(operation);
        startedAt.set(Instant.now());
        try {
            return action.get();
        } finally {
            running.set(false);
            currentOperation.set(null);
            startedAt.set(null);
        }
    }
}
