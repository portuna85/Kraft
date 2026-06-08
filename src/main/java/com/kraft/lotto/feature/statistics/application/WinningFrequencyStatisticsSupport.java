package com.kraft.lotto.feature.statistics.application;

import com.kraft.lotto.feature.statistics.infrastructure.WinningNumberFrequencySummaryEntity;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import com.kraft.lotto.feature.winningnumber.web.dto.NumberFrequencyDto;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

final class WinningFrequencyStatisticsSupport {

    static final int LOTTO_NUMBER_MIN = 1;
    static final int LOTTO_NUMBER_MAX = 45;

    private WinningFrequencyStatisticsSupport() {
    }

    static List<NumberFrequencyDto> recomputeFrequency(WinningNumberRepository repository, long totalDraws) {
        long[] counts = new long[LOTTO_NUMBER_MAX + 1];
        for (WinningNumberRepository.BallFrequencyRow row : repository.findBallFrequencies()) {
            counts[row.getBall()] = row.getHitCount();
        }
        return IntStream.rangeClosed(LOTTO_NUMBER_MIN, LOTTO_NUMBER_MAX)
                .mapToObj(n -> new NumberFrequencyDto(n, counts[n], calculateRate(counts[n], totalDraws)))
                .toList();
    }

    static List<NumberFrequencyDto> frequencyForPeriod(WinningNumberRepository repository, int minRound, long totalDraws) {
        long[] counts = new long[LOTTO_NUMBER_MAX + 1];
        for (WinningNumberRepository.BallFrequencyRow row : repository.findBallFrequenciesFromRound(minRound)) {
            counts[row.getBall()] = row.getHitCount();
        }
        return IntStream.rangeClosed(LOTTO_NUMBER_MIN, LOTTO_NUMBER_MAX)
                .mapToObj(n -> new NumberFrequencyDto(n, counts[n], calculateRate(counts[n], totalDraws)))
                .toList();
    }

    static boolean isUsableSummary(List<WinningNumberFrequencySummaryEntity> summaryRows, int latestRound) {
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
            if (row.getHitCount() < 0 || row.getLastCalculatedRound() != latestRound) {
                return false;
            }
        }
        return balls.size() == LOTTO_NUMBER_MAX;
    }

    static double calculateRate(long count, long totalDraws) {
        if (totalDraws <= 0) {
            return 0.0d;
        }
        return (count * 100.0d) / totalDraws;
    }
}
