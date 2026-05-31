package com.kraft.lotto.feature.statistics.application;

import com.kraft.lotto.feature.statistics.infrastructure.WinningNumberFrequencySummaryEntity;
import com.kraft.lotto.feature.statistics.infrastructure.WinningNumberFrequencySummaryRepository;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import com.kraft.lotto.feature.winningnumber.web.dto.CombinationPrizeHistoryDto;
import com.kraft.lotto.feature.winningnumber.web.dto.CombinationPrizeHitDto;
import com.kraft.lotto.feature.winningnumber.web.dto.CompanionNumberDto;
import com.kraft.lotto.feature.winningnumber.web.dto.NumberFrequencyDto;
import com.kraft.lotto.feature.winningnumber.web.dto.OddEvenStatDto;
import com.kraft.lotto.feature.winningnumber.web.dto.PatternStatDto;
import com.kraft.lotto.feature.winningnumber.web.dto.SumRangeStatDto;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository.CompanionRow;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository.OddEvenRow;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository.SumRow;
import com.kraft.lotto.support.BusinessException;
import com.kraft.lotto.support.ErrorCode;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.IntStream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WinningStatisticsCacheService {

    private static final int LOTTO_NUMBER_MIN = 1;
    private static final int LOTTO_NUMBER_MAX = 45;
    private static final int LOTTO_PICK_SIZE = 6;

    private final WinningNumberRepository repository;
    private final WinningNumberFrequencySummaryRepository summaryRepository;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    @Autowired
    public WinningStatisticsCacheService(WinningNumberRepository repository,
                                         WinningNumberFrequencySummaryRepository summaryRepository,
                                         ObjectProvider<MeterRegistry> meterRegistryProvider,
                                         Clock clock) {
        this(repository, summaryRepository, meterRegistryProvider.getIfAvailable(SimpleMeterRegistry::new), clock);
    }

    WinningStatisticsCacheService(WinningNumberRepository repository,
                                  WinningNumberFrequencySummaryRepository summaryRepository) {
        this(repository, summaryRepository, new SimpleMeterRegistry(), Clock.systemDefaultZone());
    }

    WinningStatisticsCacheService(WinningNumberRepository repository,
                                   WinningNumberFrequencySummaryRepository summaryRepository,
                                   MeterRegistry meterRegistry) {
        this(repository, summaryRepository, meterRegistry, Clock.systemDefaultZone());
    }

    WinningStatisticsCacheService(WinningNumberRepository repository,
                                   WinningNumberFrequencySummaryRepository summaryRepository,
                                   MeterRegistry meterRegistry,
                                   Clock clock) {
        this.repository = repository;
        this.summaryRepository = summaryRepository;
        this.meterRegistry = meterRegistry;
        this.clock = clock;
    }

    @Cacheable(cacheNames = "winningNumberFrequency", sync = true)
    @Transactional(readOnly = true)
    public List<NumberFrequencyDto> frequency() {
        long startedAt = System.nanoTime();
        String source = "recompute";
        long totalDraws = repository.count();
        if (summaryRepository != null) {
            int latestRound = repository.findMaxRound().orElse(0);
            List<WinningNumberFrequencySummaryEntity> summaryRows = summaryRepository.findAllByOrderByBallAsc();
            if (isUsableSummary(summaryRows, latestRound)) {
                List<NumberFrequencyDto> result = summaryRows.stream()
                        .map(row -> new NumberFrequencyDto(
                                row.getBall(),
                                row.getHitCount(),
                                calculateRate(row.getHitCount(), totalDraws)))
                        .toList();
                source = "summary";
                recordFrequencyLatency(startedAt, source);
                return result;
            }
        }
        countFrequencyCacheMiss(source);
        List<NumberFrequencyDto> recomputed = recomputeFrequency(totalDraws);
        recordFrequencyLatency(startedAt, source);
        return recomputed;
    }

    @Cacheable(
            cacheNames = "combinationPrizeHistory",
            keyGenerator = "combinationPrizeHistoryKeyGenerator"
    )
    @Transactional(readOnly = true)
    public CombinationPrizeHistoryDto combinationPrizeHistory(List<Integer> numbers) {
        validateCombination(numbers);
        List<Integer> normalized = numbers.stream().sorted().toList();
        List<WinningNumberRepository.PrizeHitWithRankRow> hits = repository.findPrizeHitsByNumbers(normalized.get(0),
                normalized.get(1), normalized.get(2), normalized.get(3), normalized.get(4), normalized.get(5));
        List<CombinationPrizeHitDto> firstPrizeHits = new ArrayList<>();
        List<CombinationPrizeHitDto> secondPrizeHits = new ArrayList<>();
        for (WinningNumberRepository.PrizeHitWithRankRow hit : hits) {
            CombinationPrizeHitDto dto = new CombinationPrizeHitDto(hit.getRound(), hit.getDrawDate());
            if (hit.getPrizeRank() != null && hit.getPrizeRank() == 1) {
                firstPrizeHits.add(dto);
            } else {
                secondPrizeHits.add(dto);
            }
        }
        return new CombinationPrizeHistoryDto(
                normalized,
                firstPrizeHits.size(),
                secondPrizeHits.size(),
                firstPrizeHits,
                secondPrizeHits
        );
    }

    @Cacheable(cacheNames = "winningNumberFrequencyPeriod", key = "#rounds", sync = true)
    @Transactional(readOnly = true)
    public List<NumberFrequencyDto> frequencyForPeriod(int rounds) {
        int maxRound = repository.findMaxRound().orElse(0);
        if (maxRound == 0) {
            return List.of();
        }
        int minRound = Math.max(1, maxRound - rounds + 1);
        long totalDraws = repository.countDrawsFromRound(minRound);
        long[] counts = new long[LOTTO_NUMBER_MAX + 1];
        for (WinningNumberRepository.BallFrequencyRow row : repository.findBallFrequenciesFromRound(minRound)) {
            counts[row.getBall()] = row.getHitCount();
        }
        return IntStream.rangeClosed(LOTTO_NUMBER_MIN, LOTTO_NUMBER_MAX)
                .mapToObj(n -> new NumberFrequencyDto(n, counts[n], calculateRate(counts[n], totalDraws)))
                .toList();
    }

    @Cacheable(cacheNames = "patternStats", sync = true)
    @Transactional(readOnly = true)
    public PatternStatDto patternStats() {
        long totalDraws = repository.count();

        Map<Integer, Long> oddEvenMap = new HashMap<>();
        for (OddEvenRow row : repository.findOddEvenDistribution()) {
            oddEvenMap.put(row.getOddCount(), row.getDrawCount());
        }
        long maxOddEven = oddEvenMap.values().stream().mapToLong(Long::longValue).max().orElse(1L);
        List<OddEvenStatDto> oddEvenStats = IntStream.rangeClosed(0, 6)
                .mapToObj(odd -> {
                    long cnt = oddEvenMap.getOrDefault(odd, 0L);
                    double pct = totalDraws > 0 ? cnt * 100.0 / totalDraws : 0;
                    return new OddEvenStatDto(odd, 6 - odd, cnt, pct, maxOddEven);
                })
                .toList();

        Map<Integer, Long> sumBucketMap = new TreeMap<>();
        for (SumRow row : repository.findSumDistribution()) {
            int bucket = (row.getTotalSum() / 10) * 10;
            sumBucketMap.merge(bucket, row.getDrawCount(), Long::sum);
        }
        long maxSum = sumBucketMap.values().stream().mapToLong(Long::longValue).max().orElse(1L);
        List<SumRangeStatDto> sumRangeStats = sumBucketMap.entrySet().stream()
                .map(e -> new SumRangeStatDto(e.getKey(), e.getKey() + 9, e.getValue(),
                        totalDraws > 0 ? e.getValue() * 100.0 / totalDraws : 0, maxSum))
                .toList();

        return new PatternStatDto(oddEvenStats, sumRangeStats, totalDraws);
    }

    @Cacheable(cacheNames = "companionNumbers", key = "#target", sync = true)
    @Transactional(readOnly = true)
    public List<CompanionNumberDto> companionNumbers(int target) {
        List<CompanionRow> rows = repository.findCompanionNumbers(target);
        long maxCount = rows.stream().mapToLong(CompanionRow::getHitCount).max().orElse(1L);
        List<CompanionNumberDto> result = new ArrayList<>(rows.size());
        int rank = 1;
        for (CompanionRow row : rows) {
            double pct = maxCount > 0 ? row.getHitCount() * 100.0 / maxCount : 0;
            result.add(new CompanionNumberDto(row.getOtherBall(), row.getHitCount(), pct, rank++));
        }
        return result;
    }

    @Transactional
    public void refreshFrequencySummary() {
        if (summaryRepository == null) {
            return;
        }
        long startedAt = System.nanoTime();
        long totalDraws = repository.count();
        int latestRound = repository.findMaxRound().orElse(0);
        if (latestRound == 0) {
            return;
        }
        List<NumberFrequencyDto> recomputed = recomputeFrequency(totalDraws);
        saveSummary(recomputed, latestRound);
        recordRefreshLatency(startedAt);
    }

    private List<NumberFrequencyDto> recomputeFrequency(long totalDraws) {
        long[] counts = new long[LOTTO_NUMBER_MAX + 1];
        for (WinningNumberRepository.BallFrequencyRow row : repository.findBallFrequencies()) {
            counts[row.getBall()] = row.getHitCount();
        }
        return IntStream.rangeClosed(LOTTO_NUMBER_MIN, LOTTO_NUMBER_MAX)
                .mapToObj(n -> new NumberFrequencyDto(n, counts[n], calculateRate(counts[n], totalDraws)))
                .toList();
    }

    private static double calculateRate(long count, long totalDraws) {
        if (totalDraws <= 0) {
            return 0.0d;
        }
        return (count * 100.0d) / totalDraws;
    }

    private boolean isUsableSummary(List<WinningNumberFrequencySummaryEntity> summaryRows, int latestRound) {
        if (summaryRows.size() != LOTTO_NUMBER_MAX) {
            return false;
        }
        Set<Integer> balls = new HashSet<>(LOTTO_NUMBER_MAX);
        for (WinningNumberFrequencySummaryEntity row : summaryRows) {
            Integer ball = row.getBall();
            if (ball == null || ball < LOTTO_NUMBER_MIN || ball > LOTTO_NUMBER_MAX) {
                return false;
            }
            if (!balls.add(ball)) {
                return false;
            }
            if (row.getHitCount() < 0) {
                return false;
            }
            if (row.getLastCalculatedRound() != latestRound) {
                return false;
            }
        }
        return balls.size() == LOTTO_NUMBER_MAX;
    }

    private void saveSummary(List<NumberFrequencyDto> frequencies, int latestRound) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<WinningNumberFrequencySummaryEntity> rows = frequencies.stream()
                .map(dto -> new WinningNumberFrequencySummaryEntity(dto.number(), dto.count(), latestRound, now))
                .toList();
        summaryRepository.saveAll(rows); // summaryRepository != null: caller already guards
        meterRegistry.counter("kraft.statistics.frequency.summary.refresh").increment();
    }

    private void recordFrequencyLatency(long startedAtNano, String source) {
        meterRegistry.timer("kraft.statistics.frequency.latency", "source", source)
                .record(Duration.ofNanos(System.nanoTime() - startedAtNano));
    }

    private void recordRefreshLatency(long startedAtNano) {
        meterRegistry.timer("kraft.statistics.frequency.summary.refresh.latency")
                .record(Duration.ofNanos(System.nanoTime() - startedAtNano));
    }

    private void countFrequencyCacheMiss(String source) {
        meterRegistry.counter("kraft.statistics.frequency.cache.miss", "source", source).increment();
    }

    public static String combinationPrizeHistoryCacheKey(List<Integer> numbers) {
        validateCombination(numbers);
        return String.join("-", numbers.stream()
                .sorted()
                .map(String::valueOf)
                .toList());
    }

    private static void validateCombination(List<Integer> numbers) {
        if (numbers == null || numbers.size() != LOTTO_PICK_SIZE) {
            throw new BusinessException(ErrorCode.LOTTO_INVALID_NUMBER, "numbers must contain exactly 6 values");
        }
        long seenMask = 0L;
        for (Integer number : numbers) {
            if (number == null || number < LOTTO_NUMBER_MIN || number > LOTTO_NUMBER_MAX) {
                throw new BusinessException(ErrorCode.LOTTO_INVALID_NUMBER, "numbers must be unique values from 1 to 45");
            }
            long bit = 1L << number;
            if ((seenMask & bit) != 0) {
                throw new BusinessException(ErrorCode.LOTTO_INVALID_NUMBER, "numbers must be unique values from 1 to 45");
            }
            seenMask |= bit;
        }
    }

}
