package com.kraft.statistics;

import com.kraft.common.config.CacheConfig;
import com.kraft.common.lotto.SumBuckets;
import com.kraft.winningnumber.WinningBallsOnly;
import com.kraft.winningnumber.WinningNumber;
import com.kraft.winningnumber.WinningNumberRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class WinningStatisticsCacheService {

    private static final Logger log = LoggerFactory.getLogger(WinningStatisticsCacheService.class);
    // 45개 번호 전체 쌍 조합 수(45C2=990). 클라이언트 번호별 필터가 전체 쌍을 대상으로
    // 동작하려면 전체를 전달해야 한다 — 상위 N개만 보내면 N 밖의 번호는 "기록 없음"으로 오표시된다.
    private static final int COMPANION_TOP_LIMIT = 990;

    // pattern stat_type 상수 (StatisticsSummaryRebuilder에서도 참조)
    static final String TYPE_ODD_COUNT = "ODD_COUNT";
    static final String TYPE_HIGH_COUNT = "HIGH_COUNT";
    static final String TYPE_SUM_BUCKET = "SUM_BUCKET";

    private final WinningNumberRepository winningNumberRepository;
    private final FrequencySummaryRepository frequencySummaryRepository;
    private final PatternStatsSummaryRepository patternStatsSummaryRepository;
    private final CompanionPairSummaryRepository companionPairSummaryRepository;
    private final StatisticsSummaryRebuilder summaryRebuilder;

    public WinningStatisticsCacheService(WinningNumberRepository winningNumberRepository,
                                         FrequencySummaryRepository frequencySummaryRepository,
                                         PatternStatsSummaryRepository patternStatsSummaryRepository,
                                         CompanionPairSummaryRepository companionPairSummaryRepository,
                                         StatisticsSummaryRebuilder summaryRebuilder) {
        this.winningNumberRepository = winningNumberRepository;
        this.frequencySummaryRepository = frequencySummaryRepository;
        this.patternStatsSummaryRepository = patternStatsSummaryRepository;
        this.companionPairSummaryRepository = companionPairSummaryRepository;
        this.summaryRebuilder = summaryRebuilder;
    }

    // ──────────────────────────────────────────────
    // Public API — summary → 폴백 재계산 구조
    // ──────────────────────────────────────────────

    @Cacheable(CacheConfig.STATS_FREQUENCY)
    public FrequencyStatsResponse getFrequencyStats() {
        List<FrequencySummary> summaries = frequencySummaryRepository.findAllByOrderByBallNumberAsc();

        if (summaries.isEmpty()) {
            log.info("빈도 summary 없음 — 재계산 시작");
            rebuildSummariesIgnoringConcurrencyFailure();
            summaries = frequencySummaryRepository.findAllByOrderByBallNumberAsc();
        }

        List<BallFrequencyDto> frequencies = summaries.stream()
                .map(s -> new BallFrequencyDto(s.getBallNumber(), s.getFrequency(), s.getLastRound()))
                .toList();
        return new FrequencyStatsResponse(latestRound(), frequencies);
    }

    @Cacheable(value = CacheConfig.STATS_FREQUENCY, key = "#limit")
    public FrequencyStatsResponse getFrequencyStatsByLimit(int limit) {
        List<WinningBallsOnly> rounds = winningNumberRepository
                .findBallsByOrderByRoundDesc(PageRequest.of(0, limit));
        return computeFrequencyResponse(rounds);
    }

    private FrequencyStatsResponse computeFrequencyResponse(List<WinningBallsOnly> rounds) {
        Map<Integer, Integer> freqMap = new HashMap<>();
        Map<Integer, Integer> lastRoundMap = new HashMap<>();

        for (WinningBallsOnly w : rounds) {
            for (int ball : List.of(w.getN1(), w.getN2(), w.getN3(), w.getN4(), w.getN5(), w.getN6())) {
                freqMap.merge(ball, 1, Integer::sum);
                lastRoundMap.merge(ball, w.getRound(), Math::max);
            }
        }

        List<BallFrequencyDto> frequencies = new ArrayList<>();
        for (int ball = 1; ball <= 45; ball++) {
            frequencies.add(new BallFrequencyDto(
                    ball,
                    freqMap.getOrDefault(ball, 0),
                    lastRoundMap.getOrDefault(ball, 0)
            ));
        }
        return new FrequencyStatsResponse(latestRound(), frequencies);
    }

    @Cacheable(CacheConfig.STATS_PATTERN)
    public PatternStatsResponse getPatternStats() {
        List<PatternStatsSummary> oddRows = patternStatsSummaryRepository.findByStatTypeOrderByBucketKeyAsc(TYPE_ODD_COUNT);
        if (oddRows.isEmpty()) {
            log.info("패턴 summary 없음 — 재계산 시작");
            rebuildSummariesIgnoringConcurrencyFailure();
            oddRows = patternStatsSummaryRepository.findByStatTypeOrderByBucketKeyAsc(TYPE_ODD_COUNT);
        }

        List<PatternBucketDto> oddCounts = toPatternDto(oddRows);
        List<PatternBucketDto> highCounts = toPatternDto(
                patternStatsSummaryRepository.findByStatTypeOrderByBucketKeyAsc(TYPE_HIGH_COUNT));
        List<PatternBucketDto> sumBuckets = toPatternDto(
                patternStatsSummaryRepository.findByStatTypeOrderByBucketKeyAsc(TYPE_SUM_BUCKET));

        return new PatternStatsResponse(latestRound(), oddCounts, highCounts, sumBuckets);
    }

    @Cacheable(CacheConfig.STATS_COMPANION)
    public CompanionStatsResponse getCompanionStats() {
        List<CompanionPairSummary> pairs = companionPairSummaryRepository
                .findAllByOrderByCoCountDescBallAAscBallBAsc(PageRequest.of(0, COMPANION_TOP_LIMIT));

        if (pairs.isEmpty()) {
            log.info("동반 summary 없음 — 재계산 시작");
            rebuildSummariesIgnoringConcurrencyFailure();
            pairs = companionPairSummaryRepository
                    .findAllByOrderByCoCountDescBallAAscBallBAsc(PageRequest.of(0, COMPANION_TOP_LIMIT));
        }

        List<CompanionPairDto> topPairs = pairs.stream()
                .map(p -> new CompanionPairDto(p.getBallA(), p.getBallB(), p.getCoCount()))
                .toList();
        return new CompanionStatsResponse(latestRound(), topPairs);
    }

    @Cacheable(value = CacheConfig.STATS_COMPANION, key = "#ball")
    public CompanionStatsResponse getCompanionStatsByBall(int ball) {
        List<CompanionPairSummary> pairs = companionPairSummaryRepository
                .findByBallAOrBallBOrderByCoCountDescBallAAscBallBAsc(ball, ball);

        if (pairs.isEmpty() && companionPairSummaryRepository.count() == 0) {
            log.info("동반 summary 없음 — 재계산 시작");
            rebuildSummariesIgnoringConcurrencyFailure();
            pairs = companionPairSummaryRepository
                    .findByBallAOrBallBOrderByCoCountDescBallAAscBallBAsc(ball, ball);
        }

        List<CompanionPairDto> topPairs = pairs.stream()
                .map(p -> new CompanionPairDto(p.getBallA(), p.getBallB(), p.getCoCount()))
                .toList();
        return new CompanionStatsResponse(latestRound(), topPairs);
    }

    public AnalysisResponse analyze(List<Integer> rawNumbers) {
        List<Integer> numbers = rawNumbers.stream().sorted().toList();

        int oddCount = (int) numbers.stream().filter(n -> n % 2 != 0).count();
        int evenCount = numbers.size() - oddCount;
        int highCount = (int) numbers.stream().filter(n -> n >= 23).count();
        int lowCount = numbers.size() - highCount;
        int sum = numbers.stream().mapToInt(Integer::intValue).sum();
        String sumBucket = SumBuckets.bucketOf(sum);

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

    /**
     * 캐시 미스 폴백에서 재계산이 실패해도(동시 실행 경합으로 다른 스레드가 먼저 끝낸 경우 등)
     * 호출자에게 500을 전파하지 않는다 — 직후 재조회에서 다른 스레드가 저장한 데이터를 읽을 수 있다.
     */
    private void rebuildSummariesIgnoringConcurrencyFailure() {
        try {
            summaryRebuilder.rebuildAllSummaries();
        } catch (RuntimeException ex) {
            log.warn("summary 재계산 중 예외 발생(동시 실행 경합 가능) — 기존 데이터로 폴백: {}", ex.getMessage());
        }
    }

    // ──────────────────────────────────────────────
    // 유틸리티
    // ──────────────────────────────────────────────

    private int latestRound() {
        return winningNumberRepository.findTopByOrderByRoundDesc()
                .map(WinningNumber::getRound).orElse(0);
    }

    private static List<AnalysisResponse.RangeDistribution> computeRangeDistribution(List<Integer> numbers) {
        int[] ranges = new int[5];
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
