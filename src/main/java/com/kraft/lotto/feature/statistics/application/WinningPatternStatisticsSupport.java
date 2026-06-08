package com.kraft.lotto.feature.statistics.application;

import com.kraft.lotto.feature.statistics.infrastructure.PatternStatsSummaryEntity;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import com.kraft.lotto.feature.winningnumber.web.dto.OddEvenStatDto;
import com.kraft.lotto.feature.winningnumber.web.dto.PatternStatDto;
import com.kraft.lotto.feature.winningnumber.web.dto.SumRangeStatDto;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

final class WinningPatternStatisticsSupport {

    private WinningPatternStatisticsSupport() {
    }

    static PatternStatDto buildPatternStatFromSummary(List<PatternStatsSummaryEntity> rows, int latestRound) {
        if (rows.isEmpty()) {
            return null;
        }
        List<PatternStatsSummaryEntity> oddEvenRows = rows.stream()
                .filter(r -> PatternStatsSummaryEntity.TYPE_ODD_EVEN.equals(r.getStatType()))
                .toList();
        List<PatternStatsSummaryEntity> sumRangeRows = rows.stream()
                .filter(r -> PatternStatsSummaryEntity.TYPE_SUM_RANGE.equals(r.getStatType()))
                .toList();

        if (oddEvenRows.size() != 7 || sumRangeRows.isEmpty()) {
            return null;
        }
        for (PatternStatsSummaryEntity row : rows) {
            if (row.getLastCalculatedRound() != latestRound) {
                return null;
            }
        }
        long totalDraws = oddEvenRows.get(0).getTotalDraws();
        Map<Integer, Long> oddEvenMap = oddEvenRows.stream()
                .collect(Collectors.toMap(PatternStatsSummaryEntity::getBucketKey, PatternStatsSummaryEntity::getDrawCount));
        TreeMap<Integer, Long> sumBucketMap = new TreeMap<>();
        sumRangeRows.forEach(r -> sumBucketMap.put(r.getBucketKey(), r.getDrawCount()));
        return buildPatternStat(oddEvenMap, sumBucketMap, totalDraws);
    }

    static PatternStatDto computePatternStats(WinningNumberRepository repository) {
        long totalDraws = repository.count();
        Map<Integer, Long> oddEvenMap = new HashMap<>();
        repository.findOddEvenDistribution().forEach(r -> oddEvenMap.put(r.getOddCount(), r.getDrawCount()));
        TreeMap<Integer, Long> sumBucketMap = new TreeMap<>();
        for (WinningNumberRepository.SumRow row : repository.findSumDistribution()) {
            sumBucketMap.merge((row.getTotalSum() / 10) * 10, row.getDrawCount(), Long::sum);
        }
        return buildPatternStat(oddEvenMap, sumBucketMap, totalDraws);
    }

    static PatternStatDto buildPatternStat(Map<Integer, Long> oddEvenMap, TreeMap<Integer, Long> sumBucketMap,
                                           long totalDraws) {
        long maxOddEven = oddEvenMap.values().stream().mapToLong(Long::longValue).max().orElse(1L);
        List<OddEvenStatDto> oddEvenStats = IntStream.rangeClosed(0, 6)
                .mapToObj(odd -> {
                    long cnt = oddEvenMap.getOrDefault(odd, 0L);
                    double pct = totalDraws > 0 ? cnt * 100.0 / totalDraws : 0;
                    return new OddEvenStatDto(odd, 6 - odd, cnt, pct, maxOddEven,
                            LottoTheoreticalDistribution.oddEvenPercent(odd));
                })
                .toList();
        long maxSum = sumBucketMap.values().stream().mapToLong(Long::longValue).max().orElse(1L);
        Map<Integer, Double> theoreticalSumPercents = LottoTheoreticalDistribution.sumBucketPercents(10);
        List<SumRangeStatDto> sumRangeStats = sumBucketMap.entrySet().stream()
                .map(e -> new SumRangeStatDto(e.getKey(), e.getKey() + 9, e.getValue(),
                        totalDraws > 0 ? e.getValue() * 100.0 / totalDraws : 0, maxSum,
                        theoreticalSumPercents.getOrDefault(e.getKey(), 0.0)))
                .toList();
        return new PatternStatDto(oddEvenStats, sumRangeStats, totalDraws);
    }
}
