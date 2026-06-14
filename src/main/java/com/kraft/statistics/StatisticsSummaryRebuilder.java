package com.kraft.statistics;

import com.kraft.common.config.CacheConfig;
import com.kraft.winningnumber.WinningNumber;
import com.kraft.winningnumber.WinningNumberRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StatisticsSummaryRebuilder {

    private static final Logger log = LoggerFactory.getLogger(StatisticsSummaryRebuilder.class);

    private final WinningNumberRepository winningNumberRepository;
    private final FrequencySummaryRepository frequencySummaryRepository;
    private final PatternStatsSummaryRepository patternStatsSummaryRepository;
    private final CompanionPairSummaryRepository companionPairSummaryRepository;
    private final Clock clock;

    public StatisticsSummaryRebuilder(WinningNumberRepository winningNumberRepository,
                                      FrequencySummaryRepository frequencySummaryRepository,
                                      PatternStatsSummaryRepository patternStatsSummaryRepository,
                                      CompanionPairSummaryRepository companionPairSummaryRepository,
                                      Clock clock) {
        this.winningNumberRepository = winningNumberRepository;
        this.frequencySummaryRepository = frequencySummaryRepository;
        this.patternStatsSummaryRepository = patternStatsSummaryRepository;
        this.companionPairSummaryRepository = companionPairSummaryRepository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @CacheEvict(value = {CacheConfig.STATS_FREQUENCY, CacheConfig.STATS_PATTERN, CacheConfig.STATS_COMPANION},
                allEntries = true)
    public void rebuildAllSummaries() {
        List<WinningNumber> all = winningNumberRepository.findAll();
        if (all.isEmpty()) {
            log.info("회차 데이터 없음 — summary 재계산 건너뜀");
            return;
        }

        log.info("statistics summary 재계산 시작: totalRounds={}", all.size());
        OffsetDateTime now = OffsetDateTime.now(clock);
        int latestRound = all.stream().mapToInt(WinningNumber::getRound).max().orElse(0);

        rebuildFrequency(all, latestRound, now);
        rebuildPatterns(all, now);
        rebuildCompanions(all, now);
        log.info("statistics summary 재계산 완료");
    }

    private void rebuildFrequency(List<WinningNumber> rounds, int latestRound, OffsetDateTime now) {
        Map<Integer, Integer> freqMap = new HashMap<>();
        Map<Integer, Integer> lastRoundMap = new HashMap<>();

        for (WinningNumber w : rounds) {
            for (int ball : List.of(w.getN1(), w.getN2(), w.getN3(), w.getN4(), w.getN5(), w.getN6())) {
                freqMap.merge(ball, 1, Integer::sum);
                lastRoundMap.merge(ball, w.getRound(), Math::max);
            }
        }

        Map<Integer, FrequencySummary> existing = frequencySummaryRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(FrequencySummary::getBallNumber, s -> s));

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

    private void rebuildPatterns(List<WinningNumber> rounds, OffsetDateTime now) {
        Map<String, Integer> oddCountMap = new HashMap<>();
        Map<String, Integer> highCountMap = new HashMap<>();
        Map<String, Integer> sumBucketMap = new HashMap<>();

        for (WinningNumber w : rounds) {
            List<Integer> balls = List.of(w.getN1(), w.getN2(), w.getN3(), w.getN4(), w.getN5(), w.getN6());

            String oddKey = String.valueOf(balls.stream().filter(n -> n % 2 != 0).count());
            oddCountMap.merge(oddKey, 1, Integer::sum);

            String highKey = String.valueOf(balls.stream().filter(n -> n >= 23).count());
            highCountMap.merge(highKey, 1, Integer::sum);

            int sum = balls.stream().mapToInt(Integer::intValue).sum();
            String bucket = sumBucket(sum);
            sumBucketMap.merge(bucket, 1, Integer::sum);
        }

        patternStatsSummaryRepository.deleteAllInBatch();

        List<PatternStatsSummary> toSave = new ArrayList<>();
        buildPatternRows(WinningStatisticsCacheService.TYPE_ODD_COUNT, oddCountMap, now, toSave);
        buildPatternRows(WinningStatisticsCacheService.TYPE_HIGH_COUNT, highCountMap, now, toSave);
        buildPatternRows(WinningStatisticsCacheService.TYPE_SUM_BUCKET, sumBucketMap, now, toSave);
        patternStatsSummaryRepository.saveAll(toSave);
    }

    private void buildPatternRows(String statType, Map<String, Integer> data,
                                  OffsetDateTime now, List<PatternStatsSummary> out) {
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            out.add(new PatternStatsSummary(statType, entry.getKey(), entry.getValue(), now));
        }
    }

    private void rebuildCompanions(List<WinningNumber> rounds, OffsetDateTime now) {
        Map<String, int[]> pairMap = new HashMap<>();

        for (WinningNumber w : rounds) {
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

        companionPairSummaryRepository.deleteAllInBatch();

        List<CompanionPairSummary> toSave = new ArrayList<>();
        for (int[] pair : pairMap.values()) {
            toSave.add(new CompanionPairSummary(pair[0], pair[1], pair[2], now));
        }
        companionPairSummaryRepository.saveAll(toSave);
    }

    private static String sumBucket(int sum) {
        if (sum < 66) return "21-65";
        if (sum < 111) return "66-110";
        if (sum < 156) return "111-155";
        if (sum < 201) return "156-200";
        return "201-255";
    }
}
