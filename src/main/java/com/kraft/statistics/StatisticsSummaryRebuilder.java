package com.kraft.statistics;

import com.kraft.common.config.CacheConfig;
import com.kraft.common.lotto.BallClassification;
import com.kraft.common.lotto.SumBuckets;
import com.kraft.winningnumber.WinningBallsOnly;
import com.kraft.winningnumber.WinningNumberRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class StatisticsSummaryRebuilder {

    private static final Logger log = LoggerFactory.getLogger(StatisticsSummaryRebuilder.class);
    private static final String REBUILD_LOCK_NAME = "statistics-summary-rebuild";
    private static final Duration REBUILD_LOCK_AT_MOST_FOR = Duration.ofMinutes(10);
    private static final Duration REBUILD_LOCK_AT_LEAST_FOR = Duration.ZERO;

    private final WinningNumberRepository winningNumberRepository;
    private final FrequencySummaryRepository frequencySummaryRepository;
    private final PatternStatsSummaryRepository patternStatsSummaryRepository;
    private final CompanionPairSummaryRepository companionPairSummaryRepository;
    private final Clock clock;
    private final LockingTaskExecutor lockingTaskExecutor;
    private final MeterRegistry meterRegistry;
    private final TransactionTemplate transactionTemplate;

    public StatisticsSummaryRebuilder(WinningNumberRepository winningNumberRepository,
                                      FrequencySummaryRepository frequencySummaryRepository,
                                      PatternStatsSummaryRepository patternStatsSummaryRepository,
                                      CompanionPairSummaryRepository companionPairSummaryRepository,
                                      Clock clock,
                                      LockProvider lockProvider,
                                      MeterRegistry meterRegistry,
                                      PlatformTransactionManager transactionManager) {
        this.winningNumberRepository = winningNumberRepository;
        this.frequencySummaryRepository = frequencySummaryRepository;
        this.patternStatsSummaryRepository = patternStatsSummaryRepository;
        this.companionPairSummaryRepository = companionPairSummaryRepository;
        this.clock = clock;
        this.lockingTaskExecutor = new DefaultLockingTaskExecutor(lockProvider);
        this.meterRegistry = meterRegistry;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @CacheEvict(value = {CacheConfig.STATS_FREQUENCY, CacheConfig.STATS_FREQUENCY_BY_LIMIT,
                CacheConfig.STATS_PATTERN, CacheConfig.STATS_COMPANION}, allEntries = true)
    public void rebuildAllSummaries() {
        LockingTaskExecutor.TaskResult<Boolean> result;
        try {
            result = lockingTaskExecutor.executeWithLock(
                    () -> transactionTemplate.execute(status -> rebuildAllSummariesInternal()),
                    new LockConfiguration(
                            clock.instant(),
                            REBUILD_LOCK_NAME,
                            REBUILD_LOCK_AT_MOST_FOR,
                            REBUILD_LOCK_AT_LEAST_FOR
                    )
            );
        } catch (Throwable throwable) {
            recordRebuildOutcome("failure");
            throw new IllegalStateException("Failed to rebuild statistics summaries", throwable);
        }

        if (!result.wasExecuted()) {
            log.info("statistics summary rebuild skipped because another instance holds the lock");
            recordRebuildOutcome("skipped");
        }
    }

    private boolean rebuildAllSummariesInternal() {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            List<WinningBallsOnly> all = winningNumberRepository.findAllBalls();
            if (all.isEmpty()) {
                log.info("No winning numbers found; skipping statistics summary rebuild");
                recordRebuildOutcome("empty");
                return false;
            }

            log.info("Starting statistics summary rebuild: totalRounds={}", all.size());
            OffsetDateTime now = OffsetDateTime.now(clock);

            rebuildFrequency(all, now);
            rebuildPatterns(all, now);
            rebuildCompanions(all, now);
            recordRebuildOutcome("success");
            log.info("Completed statistics summary rebuild");
            return true;
        } catch (RuntimeException exception) {
            recordRebuildOutcome("failure");
            throw exception;
        } finally {
            sample.stop(Timer.builder("statistics.summary.rebuild.duration")
                    .description("Time spent rebuilding cached statistics summaries")
                    .register(meterRegistry));
        }
    }

    private void recordRebuildOutcome(String outcome) {
        Counter.builder("statistics.summary.rebuild")
                .description("Statistics summary rebuild outcomes")
                .tag("outcome", outcome)
                .register(meterRegistry)
                .increment();
    }

    private void rebuildFrequency(List<WinningBallsOnly> rounds, OffsetDateTime now) {
        Map<Integer, Integer> freqMap = new HashMap<>();
        Map<Integer, Integer> lastRoundMap = new HashMap<>();

        for (WinningBallsOnly w : rounds) {
            for (int ball : List.of(w.getN1(), w.getN2(), w.getN3(), w.getN4(), w.getN5(), w.getN6())) {
                freqMap.merge(ball, 1, Integer::sum);
                lastRoundMap.merge(ball, w.getRound(), Math::max);
            }
        }

        Map<Integer, FrequencySummary> existing = frequencySummaryRepository.findAll().stream()
                .collect(Collectors.toMap(FrequencySummary::getBallNumber, s -> s));

        List<FrequencySummary> toSave = new ArrayList<>();
        for (int ball = 1; ball <= 45; ball++) {
            int freq = freqMap.getOrDefault(ball, 0);
            int last = lastRoundMap.getOrDefault(ball, 0);
            FrequencySummary row = existing.get(ball);
            if (row != null) {
                row.update(freq, last, now);
            } else {
                toSave.add(new FrequencySummary(ball, freq, last, now));
            }
        }
        frequencySummaryRepository.saveAll(toSave);
    }

    private void rebuildPatterns(List<WinningBallsOnly> rounds, OffsetDateTime now) {
        Map<String, Integer> oddCountMap = new HashMap<>();
        Map<String, Integer> highCountMap = new HashMap<>();
        Map<String, Integer> sumBucketMap = new HashMap<>();

        for (WinningBallsOnly w : rounds) {
            List<Integer> balls = List.of(w.getN1(), w.getN2(), w.getN3(), w.getN4(), w.getN5(), w.getN6());

            String oddKey = String.valueOf(balls.stream().filter(BallClassification::isOdd).count());
            oddCountMap.merge(oddKey, 1, Integer::sum);

            String highKey = String.valueOf(balls.stream().filter(BallClassification::isHigh).count());
            highCountMap.merge(highKey, 1, Integer::sum);

            int sum = balls.stream().mapToInt(Integer::intValue).sum();
            String bucket = SumBuckets.bucketOf(sum);
            sumBucketMap.merge(bucket, 1, Integer::sum);
        }

        Map<String, PatternStatsSummary> existing = patternStatsSummaryRepository.findAll().stream()
                .collect(Collectors.toMap(s -> patternKey(s.getStatType(), s.getBucketKey()), s -> s));

        List<PatternStatsSummary> toSave = new ArrayList<>();
        Set<String> activeKeys = new HashSet<>();
        upsertPatternRows(WinningStatisticsCacheService.TYPE_ODD_COUNT, oddCountMap, now, existing, toSave, activeKeys);
        upsertPatternRows(WinningStatisticsCacheService.TYPE_HIGH_COUNT, highCountMap, now, existing, toSave, activeKeys);
        upsertPatternRows(WinningStatisticsCacheService.TYPE_SUM_BUCKET, sumBucketMap, now, existing, toSave, activeKeys);
        patternStatsSummaryRepository.saveAll(toSave);

        List<PatternStatsSummary> stale = existing.entrySet().stream()
                .filter(entry -> !activeKeys.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();
        if (!stale.isEmpty()) {
            patternStatsSummaryRepository.deleteAllInBatch(stale);
        }
    }

    private void upsertPatternRows(String statType, Map<String, Integer> data, OffsetDateTime now,
                                   Map<String, PatternStatsSummary> existing, List<PatternStatsSummary> toSave,
                                   Set<String> activeKeys) {
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            String key = patternKey(statType, entry.getKey());
            activeKeys.add(key);
            PatternStatsSummary row = existing.get(key);
            if (row != null) {
                row.update(entry.getValue(), now);
            } else {
                toSave.add(new PatternStatsSummary(statType, entry.getKey(), entry.getValue(), now));
            }
        }
    }

    private static String patternKey(String statType, String bucketKey) {
        return statType + "::" + bucketKey;
    }

    private void rebuildCompanions(List<WinningBallsOnly> rounds, OffsetDateTime now) {
        Map<String, int[]> pairMap = new HashMap<>();

        for (WinningBallsOnly w : rounds) {
            List<Integer> balls = new ArrayList<>(
                    List.of(w.getN1(), w.getN2(), w.getN3(), w.getN4(), w.getN5(), w.getN6()));
            Collections.sort(balls);
            for (int i = 0; i < balls.size() - 1; i++) {
                for (int j = i + 1; j < balls.size(); j++) {
                    int a = balls.get(i);
                    int b = balls.get(j);
                    String key = a + "_" + b;
                    pairMap.computeIfAbsent(key, k -> new int[]{a, b, 0})[2]++;
                }
            }
        }

        Map<String, CompanionPairSummary> existing = companionPairSummaryRepository.findAll().stream()
                .collect(Collectors.toMap(s -> s.getBallA() + "_" + s.getBallB(), s -> s));

        List<CompanionPairSummary> toSave = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : pairMap.entrySet()) {
            int[] pair = entry.getValue();
            CompanionPairSummary row = existing.get(entry.getKey());
            if (row != null) {
                row.update(pair[2], now);
            } else {
                toSave.add(new CompanionPairSummary(pair[0], pair[1], pair[2], now));
            }
        }
        companionPairSummaryRepository.saveAll(toSave);

        List<CompanionPairSummary> stale = existing.entrySet().stream()
                .filter(entry -> !pairMap.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();
        if (!stale.isEmpty()) {
            companionPairSummaryRepository.deleteAllInBatch(stale);
        }
    }

}
