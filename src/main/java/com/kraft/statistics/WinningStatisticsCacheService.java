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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WinningStatisticsCacheService {

    private static final Logger log = LoggerFactory.getLogger(WinningStatisticsCacheService.class);
    private static final int COMPANION_TOP_LIMIT = 100;

    // pattern stat_type 상수
    static final String TYPE_ODD_COUNT = "ODD_COUNT";
    static final String TYPE_HIGH_COUNT = "HIGH_COUNT";
    static final String TYPE_SUM_BUCKET = "SUM_BUCKET";

    private final WinningNumberRepository winningNumberRepository;
    private final FrequencySummaryRepository frequencySummaryRepository;
    private final PatternStatsSummaryRepository patternStatsSummaryRepository;
    private final CompanionPairSummaryRepository companionPairSummaryRepository;
    private final Clock clock;

    public WinningStatisticsCacheService(WinningNumberRepository winningNumberRepository,
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

    // ──────────────────────────────────────────────
    // Public API — summary → 폴백 재계산 구조
    // ──────────────────────────────────────────────

    @Cacheable(CacheConfig.STATS_FREQUENCY)
    public FrequencyStatsResponse getFrequencyStats() {
        List<FrequencySummary> summaries = frequencySummaryRepository.findAllByOrderByBallNumberAsc();
        int totalRounds = winningNumberRepository.count() > 0
                ? winningNumberRepository.findTopByOrderByRoundDesc().map(WinningNumber::getRound).orElse(0)
                : 0;

        if (summaries.isEmpty()) {
            log.info("빈도 summary 없음 — 재계산 시작");
            rebuildAllSummaries();
            summaries = frequencySummaryRepository.findAllByOrderByBallNumberAsc();
        }

        List<BallFrequencyDto> frequencies = summaries.stream()
                .map(s -> new BallFrequencyDto(s.getBallNumber(), s.getFrequency(), s.getLastRound()))
                .toList();
        return new FrequencyStatsResponse(totalRounds, frequencies);
    }

    @Cacheable(CacheConfig.STATS_PATTERN)
    public PatternStatsResponse getPatternStats() {
        List<PatternStatsSummary> oddRows = patternStatsSummaryRepository.findByStatTypeOrderByBucketKeyAsc(TYPE_ODD_COUNT);
        if (oddRows.isEmpty()) {
            log.info("패턴 summary 없음 — 재계산 시작");
            rebuildAllSummaries();
            oddRows = patternStatsSummaryRepository.findByStatTypeOrderByBucketKeyAsc(TYPE_ODD_COUNT);
        }

        int totalRounds = winningNumberRepository.count() > 0
                ? winningNumberRepository.findTopByOrderByRoundDesc().map(WinningNumber::getRound).orElse(0)
                : 0;

        List<PatternBucketDto> oddCounts = toPatternDto(oddRows);
        List<PatternBucketDto> highCounts = toPatternDto(
                patternStatsSummaryRepository.findByStatTypeOrderByBucketKeyAsc(TYPE_HIGH_COUNT));
        List<PatternBucketDto> sumBuckets = toPatternDto(
                patternStatsSummaryRepository.findByStatTypeOrderByBucketKeyAsc(TYPE_SUM_BUCKET));

        return new PatternStatsResponse(totalRounds, oddCounts, highCounts, sumBuckets);
    }

    @Cacheable(CacheConfig.STATS_COMPANION)
    public CompanionStatsResponse getCompanionStats() {
        List<CompanionPairSummary> pairs = companionPairSummaryRepository
                .findAllByOrderByCoCountDescBallAAscBallBAsc(PageRequest.of(0, COMPANION_TOP_LIMIT));

        if (pairs.isEmpty()) {
            log.info("동반 summary 없음 — 재계산 시작");
            rebuildAllSummaries();
            pairs = companionPairSummaryRepository
                    .findAllByOrderByCoCountDescBallAAscBallBAsc(PageRequest.of(0, COMPANION_TOP_LIMIT));
        }

        int totalRounds = winningNumberRepository.count() > 0
                ? winningNumberRepository.findTopByOrderByRoundDesc().map(WinningNumber::getRound).orElse(0)
                : 0;

        List<CompanionPairDto> topPairs = pairs.stream()
                .map(p -> new CompanionPairDto(p.getBallA(), p.getBallB(), p.getCoCount()))
                .toList();
        return new CompanionStatsResponse(totalRounds, topPairs);
    }

    public AnalysisResponse analyze(List<Integer> rawNumbers) {
        List<Integer> numbers = rawNumbers.stream().sorted().toList();

        int oddCount = (int) numbers.stream().filter(n -> n % 2 != 0).count();
        int evenCount = numbers.size() - oddCount;
        // 1-22: low, 23-45: high
        int highCount = (int) numbers.stream().filter(n -> n >= 23).count();
        int lowCount = numbers.size() - highCount;
        int sum = numbers.stream().mapToInt(Integer::intValue).sum();
        String sumBucket = sumBucket(sum);

        int consecutivePairCount = 0;
        for (int i = 0; i < numbers.size() - 1; i++) {
            if (numbers.get(i + 1) - numbers.get(i) == 1) {
                consecutivePairCount++;
            }
        }

        List<AnalysisResponse.RangeDistribution> ranges = computeRangeDistribution(numbers);

        return new AnalysisResponse(numbers, oddCount, evenCount, lowCount, highCount,
                sum, sumBucket, consecutivePairCount, ranges);
    }

    // ──────────────────────────────────────────────
    // Summary 갱신 (이벤트 리스너에서 호출)
    // ──────────────────────────────────────────────

    @Transactional
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

    // ──────────────────────────────────────────────
    // 재계산 내부 로직
    // ──────────────────────────────────────────────

    private void rebuildFrequency(List<WinningNumber> rounds, int latestRound, OffsetDateTime now) {
        // 공 번호(1-45)별 출현 횟수 집계
        Map<Integer, Integer> freqMap = new HashMap<>();
        Map<Integer, Integer> lastRoundMap = new HashMap<>();

        for (WinningNumber w : rounds) {
            for (int ball : List.of(w.getN1(), w.getN2(), w.getN3(), w.getN4(), w.getN5(), w.getN6())) {
                freqMap.merge(ball, 1, Integer::sum);
                lastRoundMap.merge(ball, w.getRound(), Math::max);
            }
        }

        for (int ball = 1; ball <= 45; ball++) {
            final int freq = freqMap.getOrDefault(ball, 0);
            final int last = lastRoundMap.getOrDefault(ball, 0);
            final int ballFinal = ball;
            frequencySummaryRepository.findByBallNumber(ball)
                    .ifPresentOrElse(
                            s -> s.update(freq, last, now),
                            () -> frequencySummaryRepository.save(new FrequencySummary(ballFinal, freq, last, now))
                    );
        }
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

        upsertPatternRows(TYPE_ODD_COUNT, oddCountMap, now);
        upsertPatternRows(TYPE_HIGH_COUNT, highCountMap, now);
        upsertPatternRows(TYPE_SUM_BUCKET, sumBucketMap, now);
    }

    private void upsertPatternRows(String statType, Map<String, Integer> data, OffsetDateTime now) {
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            patternStatsSummaryRepository
                    .findByStatTypeAndBucketKey(statType, entry.getKey())
                    .ifPresentOrElse(
                            s -> s.update(entry.getValue(), now),
                            () -> patternStatsSummaryRepository.save(
                                    new PatternStatsSummary(statType, entry.getKey(), entry.getValue(), now))
                    );
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

        for (int[] pair : pairMap.values()) {
            int a = pair[0], b = pair[1], count = pair[2];
            companionPairSummaryRepository.findByBallAAndBallB(a, b)
                    .ifPresentOrElse(
                            s -> s.update(count, now),
                            () -> companionPairSummaryRepository.save(new CompanionPairSummary(a, b, count, now))
                    );
        }
    }

    // ──────────────────────────────────────────────
    // 유틸리티
    // ──────────────────────────────────────────────

    private static String sumBucket(int sum) {
        if (sum < 66) {
            return "21-65";
        }
        if (sum < 111) {
            return "66-110";
        }
        if (sum < 156) {
            return "111-155";
        }
        if (sum < 201) {
            return "156-200";
        }
        return "201-255";
    }

    private static List<AnalysisResponse.RangeDistribution> computeRangeDistribution(List<Integer> numbers) {
        int[] ranges = new int[5]; // 1-9, 10-19, 20-29, 30-39, 40-45
        for (int n : numbers) {
            if (n <= 9) {
                ranges[0]++;
            } else if (n <= 19) {
                ranges[1]++;
            } else if (n <= 29) {
                ranges[2]++;
            } else if (n <= 39) {
                ranges[3]++;
            } else {
                ranges[4]++;
            }
        }
        return List.of(
                new AnalysisResponse.RangeDistribution("1-9", ranges[0]),
                new AnalysisResponse.RangeDistribution("10-19", ranges[1]),
                new AnalysisResponse.RangeDistribution("20-29", ranges[2]),
                new AnalysisResponse.RangeDistribution("30-39", ranges[3]),
                new AnalysisResponse.RangeDistribution("40-45", ranges[4])
        );
    }

    private static List<PatternBucketDto> toPatternDto(List<PatternStatsSummary> rows) {
        return rows.stream()
                .map(r -> new PatternBucketDto(r.getBucketKey(), r.getCountVal()))
                .toList();
    }
}
