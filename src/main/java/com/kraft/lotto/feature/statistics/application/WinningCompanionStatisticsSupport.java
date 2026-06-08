package com.kraft.lotto.feature.statistics.application;

import com.kraft.lotto.feature.statistics.infrastructure.CompanionPairSummaryEntity;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import com.kraft.lotto.feature.winningnumber.web.dto.CompanionNumberDto;
import java.util.ArrayList;
import java.util.List;

final class WinningCompanionStatisticsSupport {

    private WinningCompanionStatisticsSupport() {
    }

    static boolean isUsableSummary(List<CompanionPairSummaryEntity> rows, int latestRound) {
        if (rows.size() != WinningFrequencyStatisticsSupport.LOTTO_NUMBER_MAX - 1) {
            return false;
        }
        for (CompanionPairSummaryEntity row : rows) {
            if (row.getLastCalculatedRound() != latestRound) {
                return false;
            }
        }
        return true;
    }

    static List<CompanionNumberDto> buildDtosFromSummary(List<CompanionPairSummaryEntity> rows) {
        long maxCount = rows.stream().mapToLong(CompanionPairSummaryEntity::getHitCount).max().orElse(1L);
        List<CompanionNumberDto> result = new ArrayList<>(rows.size());
        long prevCount = Long.MIN_VALUE;
        int denseRank = 0;
        for (CompanionPairSummaryEntity row : rows) {
            if (row.getHitCount() != prevCount) {
                denseRank++;
                prevCount = row.getHitCount();
            }
            double pct = maxCount > 0 ? row.getHitCount() * 100.0 / maxCount : 0;
            result.add(new CompanionNumberDto(row.getOtherBall(), row.getHitCount(), pct, denseRank));
        }
        return result;
    }

    static List<CompanionNumberDto> buildDtosFromRows(List<WinningNumberRepository.CompanionRow> rows) {
        long maxCount = rows.stream().mapToLong(WinningNumberRepository.CompanionRow::getHitCount).max().orElse(1L);
        List<CompanionNumberDto> result = new ArrayList<>(rows.size());
        long prevCount = Long.MIN_VALUE;
        int denseRank = 0;
        for (WinningNumberRepository.CompanionRow row : rows) {
            if (row.getHitCount() != prevCount) {
                denseRank++;
                prevCount = row.getHitCount();
            }
            double pct = maxCount > 0 ? row.getHitCount() * 100.0 / maxCount : 0;
            result.add(new CompanionNumberDto(row.getOtherBall(), row.getHitCount(), pct, denseRank));
        }
        return result;
    }
}
