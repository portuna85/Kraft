package com.kraft.statistics;

import com.kraft.common.config.CacheConfig;
import com.kraft.winningnumber.WinningNumber;
import com.kraft.winningnumber.WinningNumberRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WinningStatisticsCacheService {

    private static final Logger log = LoggerFactory.getLogger(WinningStatisticsCacheService.class);
    private static final int COMPANION_TOP_LIMIT = 100;

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
            summaryRebuilder.rebuildAllSummaries();
            summaries = frequencySummaryRepository.findAllByOrderByBallNumberAsc();
        }

        List<BallFrequencyDto> frequencies = summaries.stream()
                .map(s -> new BallFrequencyDto(s.getBallNumber(), s.getFrequency(), s.getLastRound()))
                .toList();
        return new FrequencyStatsResponse(latestRound(), frequencies);
    }

    @Cacheable(CacheConfig.STATS_PATTERN)
    public PatternStatsResponse getPatternStats() {
        List<PatternStatsSummary> oddRows = patternStatsSummaryRepository.findByStatTypeOrderByBucketKeyAsc(TYPE_ODD_COUNT);
        if (oddRows.isEmpty()) {
            log.info("패턴 summary 없음 — 재계산 시작");
            summaryRebuilder.rebuildAllSummaries();
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
            summaryRebuilder.rebuildAllSummaries();
            pairs = companionPairSummaryRepository
                    .findAllByOrderByCoCountDescBallAAscBallBAsc(PageRequest.of(0, COMPANION_TOP_LIMIT));
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
    // 유틸리티
    // ──────────────────────────────────────────────

    private int latestRound() {
        return winningNumberRepository.findTopByOrderByRoundDesc()
                .map(WinningNumber::getRound).orElse(0);
    }

    private static String sumBucket(int sum) {
        if (sum < 66) return "21-65";
        if (sum < 111) return "66-110";
        if (sum < 156) return "111-155";
        if (sum < 201) return "156-200";
        return "201-255";
    }

    private static List<AnalysisResponse.RangeDistribution> computeRangeDistribution(List<Integer> numbers) {
        int[] ranges = new int[5];
        for (int n : numbers) {
            if (n <= 9) ranges[0]++;
            else if (n <= 19) ranges[1]++;
            else if (n <= 29) ranges[2]++;
            else if (n <= 39) ranges[3]++;
            else ranges[4]++;
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
