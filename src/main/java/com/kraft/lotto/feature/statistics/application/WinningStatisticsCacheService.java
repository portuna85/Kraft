package com.kraft.lotto.feature.statistics.application;

import com.kraft.lotto.feature.statistics.infrastructure.CompanionPairSummaryEntity;
import com.kraft.lotto.feature.statistics.infrastructure.CompanionPairSummaryRepository;
import com.kraft.lotto.feature.statistics.infrastructure.PatternStatsSummaryEntity;
import com.kraft.lotto.feature.statistics.infrastructure.PatternStatsSummaryRepository;
import com.kraft.lotto.feature.statistics.infrastructure.WinningNumberFrequencySummaryEntity;
import com.kraft.lotto.feature.statistics.infrastructure.WinningNumberFrequencySummaryRepository;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import com.kraft.lotto.feature.winningnumber.web.dto.CombinationPrizeHistoryDto;
import com.kraft.lotto.feature.winningnumber.web.dto.CombinationPrizeHitDto;
import com.kraft.lotto.feature.winningnumber.web.dto.CompanionNumberDto;
import com.kraft.lotto.feature.winningnumber.web.dto.NumberFrequencyDto;
import com.kraft.lotto.feature.winningnumber.web.dto.PatternStatDto;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberStatisticsRepository.OddEvenRow;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberStatisticsRepository.SumRow;
import com.kraft.lotto.support.BusinessException;
import com.kraft.lotto.support.ErrorCode;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WinningStatisticsCacheService {

    private static final int LOTTO_PICK_SIZE = 6;

    private final WinningNumberRepository repository;
    private final WinningNumberFrequencySummaryRepository summaryRepository;
    private final PatternStatsSummaryRepository patternStatsSummaryRepository;
    private final CompanionPairSummaryRepository companionPairSummaryRepository;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    @Autowired
    public WinningStatisticsCacheService(WinningNumberRepository repository,
                                         WinningNumberFrequencySummaryRepository summaryRepository,
                                         ObjectProvider<MeterRegistry> meterRegistryProvider,
                                         Clock clock,
                                         PatternStatsSummaryRepository patternStatsSummaryRepository,
                                         CompanionPairSummaryRepository companionPairSummaryRepository) {
        this(repository, summaryRepository,
                meterRegistryProvider.getIfAvailable(SimpleMeterRegistry::new),
                clock,
                patternStatsSummaryRepository,
                companionPairSummaryRepository);
    }

    WinningStatisticsCacheService(WinningNumberRepository repository,
                                  WinningNumberFrequencySummaryRepository summaryRepository) {
        this(repository, summaryRepository, new SimpleMeterRegistry(), Clock.systemDefaultZone(), null, null);
    }

    WinningStatisticsCacheService(WinningNumberRepository repository,
                                  WinningNumberFrequencySummaryRepository summaryRepository,
                                  PatternStatsSummaryRepository patternStatsSummaryRepository,
                                  CompanionPairSummaryRepository companionPairSummaryRepository) {
        this(repository, summaryRepository, new SimpleMeterRegistry(), Clock.systemDefaultZone(),
                patternStatsSummaryRepository, companionPairSummaryRepository);
    }

    WinningStatisticsCacheService(WinningNumberRepository repository,
                                   WinningNumberFrequencySummaryRepository summaryRepository,
                                   MeterRegistry meterRegistry) {
        this(repository, summaryRepository, meterRegistry, Clock.systemDefaultZone(), null, null);
    }

    WinningStatisticsCacheService(WinningNumberRepository repository,
                                   WinningNumberFrequencySummaryRepository summaryRepository,
                                   MeterRegistry meterRegistry,
                                   Clock clock) {
        this(repository, summaryRepository, meterRegistry, clock, null, null);
    }

    WinningStatisticsCacheService(WinningNumberRepository repository,
                                   WinningNumberFrequencySummaryRepository summaryRepository,
                                   MeterRegistry meterRegistry,
                                   Clock clock,
                                   PatternStatsSummaryRepository patternStatsSummaryRepository,
                                   CompanionPairSummaryRepository companionPairSummaryRepository) {
        this.repository = repository;
        this.summaryRepository = summaryRepository;
        this.meterRegistry = meterRegistry;
        this.clock = clock;
        this.patternStatsSummaryRepository = patternStatsSummaryRepository;
        this.companionPairSummaryRepository = companionPairSummaryRepository;
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
            if (WinningFrequencyStatisticsSupport.isUsableSummary(summaryRows, latestRound)) {
                List<NumberFrequencyDto> result = summaryRows.stream()
                        .map(row -> new NumberFrequencyDto(
                                row.getBall(),
                                row.getHitCount(),
                                WinningFrequencyStatisticsSupport.calculateRate(row.getHitCount(), totalDraws)))
                        .toList();
                source = "summary";
                recordFrequencyLatency(startedAt, source);
                return result;
            }
        }
        countFrequencyCacheMiss(source);
        List<NumberFrequencyDto> recomputed = WinningFrequencyStatisticsSupport.recomputeFrequency(repository, totalDraws);
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
        List<com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberStatisticsRepository.PrizeHitWithRankRow> hits =
                repository.findPrizeHitsByNumbers(normalized.get(0),
                normalized.get(1), normalized.get(2), normalized.get(3), normalized.get(4), normalized.get(5));
        List<CombinationPrizeHitDto> firstPrizeHits = new ArrayList<>();
        List<CombinationPrizeHitDto> secondPrizeHits = new ArrayList<>();
        for (com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberStatisticsRepository.PrizeHitWithRankRow hit : hits) {
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
        if (rounds <= 0) {
            throw new BusinessException(ErrorCode.LOTTO_INVALID_NUMBER, "rounds must be positive");
        }
        int maxRound = repository.findMaxRound().orElse(0);
        if (maxRound == 0) {
            return List.of();
        }
        int minRound = Math.max(1, maxRound - rounds + 1);
        long totalDraws = repository.countDrawsFromRound(minRound);
        return WinningFrequencyStatisticsSupport.frequencyForPeriod(repository, minRound, totalDraws);
    }

    @Cacheable(cacheNames = "patternStats", sync = true)
    @Transactional(readOnly = true)
    public PatternStatDto patternStats() {
        if (patternStatsSummaryRepository != null) {
            int latestRound = repository.findMaxRound().orElse(0);
            if (latestRound > 0) {
                List<PatternStatsSummaryEntity> summaryRows =
                        patternStatsSummaryRepository.findAllByOrderByStatTypeAscBucketKeyAsc();
                PatternStatDto fromSummary = WinningPatternStatisticsSupport.buildPatternStatFromSummary(summaryRows, latestRound);
                if (fromSummary != null) {
                    return fromSummary;
                }
            }
        }
        return WinningPatternStatisticsSupport.computePatternStats(repository);
    }

    @Cacheable(cacheNames = "companionNumbers", key = "#target", sync = true)
    @Transactional(readOnly = true)
    public List<CompanionNumberDto> companionNumbers(int target) {
        if (target < WinningFrequencyStatisticsSupport.LOTTO_NUMBER_MIN
                || target > WinningFrequencyStatisticsSupport.LOTTO_NUMBER_MAX) {
            throw new BusinessException(ErrorCode.LOTTO_INVALID_NUMBER, "target must be between 1 and 45");
        }
        if (companionPairSummaryRepository != null) {
            int latestRound = repository.findMaxRound().orElse(0);
            if (latestRound > 0) {
                List<CompanionPairSummaryEntity> summaryRows =
                        companionPairSummaryRepository.findByBallOrderByHitCountDesc(target);
                if (WinningCompanionStatisticsSupport.isUsableSummary(summaryRows, latestRound)) {
                    return WinningCompanionStatisticsSupport.buildDtosFromSummary(summaryRows);
                }
            }
        }
        return WinningCompanionStatisticsSupport.buildDtosFromRows(repository.findCompanionNumbers(target));
    }

    public boolean refreshAll() {
        for (String type : new String[]{"frequency", "pattern", "companion"}) {
            try {
                switch (type) {
                    case "frequency" -> refreshFrequencySummary();
                    case "pattern"   -> refreshPatternStatsSummary();
                    case "companion" -> refreshCompanionPairSummary();
                    default -> throw new IllegalStateException("unknown type: " + type);
                }
                meterRegistry.counter("kraft.statistics.summary.refresh", "type", type, "result", "ok").increment();
            } catch (RuntimeException e) {
                meterRegistry.counter("kraft.statistics.summary.refresh", "type", type, "result", "failed").increment();
                return false;
            }
        }
        return true;
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
        List<NumberFrequencyDto> recomputed = WinningFrequencyStatisticsSupport.recomputeFrequency(repository, totalDraws);
        saveSummary(recomputed, latestRound);
        recordRefreshLatency(startedAt);
    }

    @Transactional
    public void refreshPatternStatsSummary() {
        if (patternStatsSummaryRepository == null) {
            return;
        }
        long startedAt = System.nanoTime();
        int latestRound = repository.findMaxRound().orElse(0);
        if (latestRound == 0) {
            return;
        }
        long totalDraws = repository.count();
        LocalDateTime now = LocalDateTime.now(clock);
        List<PatternStatsSummaryEntity> rows = new ArrayList<>();

        for (OddEvenRow row : repository.findOddEvenDistribution()) {
            rows.add(new PatternStatsSummaryEntity(
                    PatternStatsSummaryEntity.TYPE_ODD_EVEN,
                    row.getOddCount(),
                    row.getDrawCount(),
                    totalDraws,
                    latestRound,
                    now));
        }

        Map<Integer, Long> sumBucketMap = new TreeMap<>();
        for (SumRow row : repository.findSumDistribution()) {
            int bucket = (row.getTotalSum() / 10) * 10;
            sumBucketMap.merge(bucket, row.getDrawCount(), Long::sum);
        }
        for (Map.Entry<Integer, Long> entry : sumBucketMap.entrySet()) {
            rows.add(new PatternStatsSummaryEntity(
                    PatternStatsSummaryEntity.TYPE_SUM_RANGE,
                    entry.getKey(),
                    entry.getValue(),
                    totalDraws,
                    latestRound,
                    now));
        }

        patternStatsSummaryRepository.saveAll(rows);
        meterRegistry.counter("kraft.statistics.pattern.summary.refresh").increment();
        meterRegistry.timer("kraft.statistics.summary.refresh.duration", "type", "pattern")
                .record(Duration.ofNanos(System.nanoTime() - startedAt));
    }

    @Transactional
    public void refreshCompanionPairSummary() {
        if (companionPairSummaryRepository == null) {
            return;
        }
        long startedAt = System.nanoTime();
        int latestRound = repository.findMaxRound().orElse(0);
        if (latestRound == 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        List<CompanionPairSummaryEntity> rows = repository.findAllCompanionPairs().stream()
                .map(pair -> new CompanionPairSummaryEntity(
                        pair.getBall(),
                        pair.getOtherBall(),
                        pair.getHitCount(),
                        latestRound,
                        now))
                .toList();
        companionPairSummaryRepository.saveAll(rows);
        meterRegistry.counter("kraft.statistics.companion.summary.refresh").increment();
        meterRegistry.timer("kraft.statistics.summary.refresh.duration", "type", "companion")
                .record(Duration.ofNanos(System.nanoTime() - startedAt));
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
        meterRegistry.timer("kraft.statistics.summary.refresh.duration", "type", "frequency")
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
            if (number == null
                    || number < WinningFrequencyStatisticsSupport.LOTTO_NUMBER_MIN
                    || number > WinningFrequencyStatisticsSupport.LOTTO_NUMBER_MAX) {
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
