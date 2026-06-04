package com.kraft.lotto.feature.statistics.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kraft.lotto.feature.statistics.infrastructure.CompanionPairSummaryEntity;
import com.kraft.lotto.feature.statistics.infrastructure.CompanionPairSummaryRepository;
import com.kraft.lotto.feature.statistics.infrastructure.PatternStatsSummaryEntity;
import com.kraft.lotto.feature.statistics.infrastructure.PatternStatsSummaryRepository;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import com.kraft.lotto.feature.winningnumber.web.dto.CompanionNumberDto;
import com.kraft.lotto.feature.winningnumber.web.dto.PatternStatDto;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("통계 summary 테이블 경로 테스트")
class WinningStatisticsSummaryTest {

    private static final int LATEST_ROUND = 1100;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 4, 0, 0);

    @Mock
    WinningNumberRepository repository;
    @Mock
    PatternStatsSummaryRepository patternStatsSummaryRepository;
    @Mock
    CompanionPairSummaryRepository companionPairSummaryRepository;

    WinningStatisticsCacheService service;

    @BeforeEach
    void setUp() {
        service = new WinningStatisticsCacheService(
                repository, null,
                patternStatsSummaryRepository, companionPairSummaryRepository);
    }

    // ──── patternStats ────

    @Test
    @DisplayName("유효한 pattern summary가 있으면 DB 집계 쿼리를 호출하지 않는다")
    void patternStatsUsesSummaryWhenValid() {
        when(repository.findMaxRound()).thenReturn(Optional.of(LATEST_ROUND));
        when(patternStatsSummaryRepository.findAllByOrderByStatTypeAscBucketKeyAsc())
                .thenReturn(validPatternSummaryRows(LATEST_ROUND));

        PatternStatDto result = service.patternStats();

        assertThat(result.oddEvenStats()).hasSize(7);
        assertThat(result.totalDraws()).isEqualTo(1000L);
        verify(repository, never()).findOddEvenDistribution();
        verify(repository, never()).findSumDistribution();
    }

    @Test
    @DisplayName("summary round가 최신이 아니면 DB 집계로 fallback 한다")
    void patternStatsFallbackWhenSummaryStale() {
        when(repository.findMaxRound()).thenReturn(Optional.of(LATEST_ROUND));
        when(patternStatsSummaryRepository.findAllByOrderByStatTypeAscBucketKeyAsc())
                .thenReturn(validPatternSummaryRows(LATEST_ROUND - 1));
        when(repository.count()).thenReturn(1000L);
        when(repository.findOddEvenDistribution()).thenReturn(List.of());
        when(repository.findSumDistribution()).thenReturn(List.of());

        service.patternStats();

        verify(repository).findOddEvenDistribution();
        verify(repository).findSumDistribution();
    }

    @Test
    @DisplayName("ODD_EVEN 행이 7개 미만이면 DB 집계로 fallback 한다")
    void patternStatsFallbackWhenOddEvenRowsInsufficient() {
        when(repository.findMaxRound()).thenReturn(Optional.of(LATEST_ROUND));
        List<PatternStatsSummaryEntity> incomplete = validPatternSummaryRows(LATEST_ROUND);
        incomplete.removeIf(r -> PatternStatsSummaryEntity.TYPE_ODD_EVEN.equals(r.getStatType())
                && r.getBucketKey() == 6);
        when(patternStatsSummaryRepository.findAllByOrderByStatTypeAscBucketKeyAsc())
                .thenReturn(incomplete);
        when(repository.count()).thenReturn(1000L);
        when(repository.findOddEvenDistribution()).thenReturn(List.of());
        when(repository.findSumDistribution()).thenReturn(List.of());

        service.patternStats();

        verify(repository).findOddEvenDistribution();
    }

    // ──── companionNumbers ────

    @Test
    @DisplayName("유효한 companion summary가 있으면 DB UNION ALL 쿼리를 호출하지 않는다")
    void companionNumbersUsesSummaryWhenValid() {
        int target = 7;
        when(repository.findMaxRound()).thenReturn(Optional.of(LATEST_ROUND));
        when(companionPairSummaryRepository.findByBallOrderByHitCountDesc(target))
                .thenReturn(validCompanionSummaryRows(target, LATEST_ROUND));

        List<CompanionNumberDto> result = service.companionNumbers(target);

        assertThat(result).hasSize(44);
        verify(repository, never()).findCompanionNumbers(target);
    }

    @Test
    @DisplayName("companion summary row 수가 44개 미만이면 DB 쿼리로 fallback 한다")
    void companionNumbersFallbackWhenSummaryIncomplete() {
        int target = 7;
        when(repository.findMaxRound()).thenReturn(Optional.of(LATEST_ROUND));
        when(companionPairSummaryRepository.findByBallOrderByHitCountDesc(target))
                .thenReturn(List.of());
        when(repository.findCompanionNumbers(target)).thenReturn(List.of());

        service.companionNumbers(target);

        verify(repository).findCompanionNumbers(target);
    }

    @Test
    @DisplayName("companion summary round가 최신이 아니면 DB 쿼리로 fallback 한다")
    void companionNumbersFallbackWhenSummaryStale() {
        int target = 3;
        when(repository.findMaxRound()).thenReturn(Optional.of(LATEST_ROUND));
        when(companionPairSummaryRepository.findByBallOrderByHitCountDesc(target))
                .thenReturn(validCompanionSummaryRows(target, LATEST_ROUND - 1));
        when(repository.findCompanionNumbers(target)).thenReturn(List.of());

        service.companionNumbers(target);

        verify(repository).findCompanionNumbers(target);
    }

    @Test
    @DisplayName("companion summary dense rank이 올바르게 계산된다")
    void companionNumbersSummaryDenseRankCorrect() {
        int target = 7;
        when(repository.findMaxRound()).thenReturn(Optional.of(LATEST_ROUND));

        List<CompanionPairSummaryEntity> rows = new ArrayList<>();
        rows.add(summaryPair(target, 1, 50L, LATEST_ROUND));
        rows.add(summaryPair(target, 2, 50L, LATEST_ROUND));
        rows.add(summaryPair(target, 3, 40L, LATEST_ROUND));
        for (int other = 4; other <= 45; other++) {
            if (other == target) {
                continue;
            }
            rows.add(summaryPair(target, other, 30L, LATEST_ROUND));
        }
        when(companionPairSummaryRepository.findByBallOrderByHitCountDesc(target)).thenReturn(rows);

        List<CompanionNumberDto> result = service.companionNumbers(target);

        assertThat(result.get(0).rank()).isEqualTo(1);
        assertThat(result.get(1).rank()).isEqualTo(1);
        assertThat(result.get(2).rank()).isEqualTo(2);
    }

    // ──── refresh ────

    @Test
    @DisplayName("refreshPatternStatsSummary는 ODD_EVEN 7행과 SUM_RANGE 행을 저장한다")
    void refreshPatternStatsSummarySavesAllTypes() {
        when(repository.findMaxRound()).thenReturn(Optional.of(LATEST_ROUND));
        when(repository.count()).thenReturn(1000L);
        when(repository.findOddEvenDistribution()).thenReturn(oddEvenRows());
        when(repository.findSumDistribution()).thenReturn(sumRows());

        service.refreshPatternStatsSummary();

        verify(patternStatsSummaryRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("refreshPatternStatsSummary는 maxRound 0이면 아무것도 저장하지 않는다")
    void refreshPatternStatsSummarySkipsWhenNoData() {
        when(repository.findMaxRound()).thenReturn(Optional.empty());

        service.refreshPatternStatsSummary();

        verify(patternStatsSummaryRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("refreshCompanionPairSummary는 findAllCompanionPairs 결과를 저장한다")
    void refreshCompanionPairSummarySavesAllPairs() {
        when(repository.findMaxRound()).thenReturn(Optional.of(LATEST_ROUND));
        when(repository.findAllCompanionPairs()).thenReturn(List.of(
                companionPairRow(1, 2, 100L),
                companionPairRow(1, 3, 90L)
        ));

        service.refreshCompanionPairSummary();

        verify(companionPairSummaryRepository).saveAll(anyList());
    }

    // ──── helpers ────

    private static List<PatternStatsSummaryEntity> validPatternSummaryRows(int round) {
        List<PatternStatsSummaryEntity> rows = new ArrayList<>();
        for (int odd = 0; odd <= 6; odd++) {
            rows.add(new PatternStatsSummaryEntity(
                    PatternStatsSummaryEntity.TYPE_ODD_EVEN, odd, 100L, 1000L, round, NOW));
        }
        rows.add(new PatternStatsSummaryEntity(
                PatternStatsSummaryEntity.TYPE_SUM_RANGE, 100, 200L, 1000L, round, NOW));
        rows.add(new PatternStatsSummaryEntity(
                PatternStatsSummaryEntity.TYPE_SUM_RANGE, 110, 300L, 1000L, round, NOW));
        return rows;
    }

    private static List<CompanionPairSummaryEntity> validCompanionSummaryRows(int ball, int round) {
        List<CompanionPairSummaryEntity> rows = new ArrayList<>();
        int hitCount = 100;
        for (int n = 1; n <= 45; n++) {
            if (n == ball) {
                continue;
            }
            rows.add(summaryPair(ball, n, hitCount--, round));
        }
        return rows;
    }

    private static CompanionPairSummaryEntity summaryPair(int ball, int otherBall, long hitCount, int round) {
        return new CompanionPairSummaryEntity(ball, otherBall, hitCount, round, NOW);
    }

    private static List<WinningNumberRepository.OddEvenRow> oddEvenRows() {
        List<WinningNumberRepository.OddEvenRow> rows = new ArrayList<>();
        for (int odd = 0; odd <= 6; odd++) {
            final int oddCount = odd;
            rows.add(new WinningNumberRepository.OddEvenRow() {
                public Integer getOddCount() { return oddCount; }
                public Long getDrawCount() { return 100L; }
            });
        }
        return rows;
    }

    private static List<WinningNumberRepository.SumRow> sumRows() {
        return List.of(new WinningNumberRepository.SumRow() {
            public Integer getTotalSum() { return 105; }
            public Long getDrawCount() { return 50L; }
        });
    }

    private static WinningNumberRepository.CompanionPairRow companionPairRow(int ball, int other, long hitCount) {
        return new WinningNumberRepository.CompanionPairRow() {
            public Integer getBall() { return ball; }
            public Integer getOtherBall() { return other; }
            public Long getHitCount() { return hitCount; }
        };
    }
}
