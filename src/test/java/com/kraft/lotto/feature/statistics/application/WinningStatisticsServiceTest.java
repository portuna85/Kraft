package com.kraft.lotto.feature.statistics.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kraft.lotto.feature.statistics.infrastructure.WinningNumberFrequencySummaryEntity;
import com.kraft.lotto.feature.statistics.infrastructure.WinningNumberFrequencySummaryRepository;
import com.kraft.lotto.feature.winningnumber.event.WinningNumbersCollectedEvent;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import com.kraft.lotto.feature.winningnumber.web.dto.NumberFrequencyDto;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

@ExtendWith(MockitoExtension.class)
@DisplayName("당첨 통계 서비스 테스트")
class WinningStatisticsServiceTest {

    @Mock
    WinningNumberRepository winningNumberRepository;

    @Mock
    WinningNumberFrequencySummaryRepository summaryRepository;

    // ---- WinningStatisticsCacheService 위임 로직 테스트 ----

    @Test
    @DisplayName("요약 데이터가 최신이면 요약 테이블을 사용한다")
    void usesSummaryWhenUpToDate() {
        when(winningNumberRepository.findMaxRound()).thenReturn(Optional.of(1200));
        List<WinningNumberFrequencySummaryEntity> rows = new ArrayList<>();
        for (int i = 1; i <= 45; i++) {
            rows.add(new WinningNumberFrequencySummaryEntity(i, i * 2L, 1200));
        }
        when(summaryRepository.findAllByOrderByBallAsc()).thenReturn(rows);

        WinningStatisticsCacheService cacheService = new WinningStatisticsCacheService(winningNumberRepository, summaryRepository);
        var result = cacheService.frequency();

        assertThat(result).hasSize(45);
        assertThat(result.get(0).number()).isEqualTo(1);
        assertThat(result.get(0).count()).isEqualTo(2L);
        verify(winningNumberRepository, never()).findBallFrequencies();
    }

    @Test
    @DisplayName("요약 데이터가 오래된 경우 DB에 쓰지 않고 재계산한다")
    void recomputesWhenSummaryIsStale() {
        when(winningNumberRepository.findMaxRound()).thenReturn(Optional.of(1201));
        when(summaryRepository.findAllByOrderByBallAsc()).thenReturn(List.of(
                new WinningNumberFrequencySummaryEntity(1, 10L, 1200)
        ));
        when(winningNumberRepository.findBallFrequencies()).thenReturn(List.of(
                ballFrequencyRow(1, 2L), ballFrequencyRow(2, 2L), ballFrequencyRow(3, 2L),
                ballFrequencyRow(4, 2L), ballFrequencyRow(5, 2L), ballFrequencyRow(6, 2L)
        ));

        WinningStatisticsCacheService cacheService = new WinningStatisticsCacheService(winningNumberRepository, summaryRepository);
        var result = cacheService.frequency();

        assertThat(result).hasSize(45);
        assertThat(result.get(0).count()).isEqualTo(2L);
        verify(summaryRepository, never()).saveAll(ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("빈도 요약 갱신 시 재계산된 결과를 저장하고 메트릭을 기록한다")
    void refreshFrequencySummarySavesAndRecordsMetrics() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        when(winningNumberRepository.findMaxRound()).thenReturn(Optional.of(1201));
        when(winningNumberRepository.findBallFrequencies()).thenReturn(List.of(
                ballFrequencyRow(1, 1L), ballFrequencyRow(2, 1L), ballFrequencyRow(3, 1L),
                ballFrequencyRow(4, 1L), ballFrequencyRow(5, 1L), ballFrequencyRow(6, 1L)
        ));

        WinningStatisticsCacheService cacheService =
                new WinningStatisticsCacheService(winningNumberRepository, summaryRepository, meterRegistry);
        cacheService.refreshFrequencySummary();

        verify(summaryRepository).saveAll(ArgumentMatchers.anyList());
        double refreshCount = meterRegistry.get("kraft.statistics.frequency.summary.refresh")
                .counter().count();
        assertThat(refreshCount).isEqualTo(1.0);
    }

    @Test
    @DisplayName("재계산 시 지연 시간 메트릭을 기록한다")
    void recordsLatencyMetricOnRecompute() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        when(winningNumberRepository.findMaxRound()).thenReturn(Optional.of(1201));
        when(summaryRepository.findAllByOrderByBallAsc()).thenReturn(List.of(
                new WinningNumberFrequencySummaryEntity(1, 10L, 1200)
        ));
        when(winningNumberRepository.findBallFrequencies()).thenReturn(List.of(
                ballFrequencyRow(1, 1L), ballFrequencyRow(2, 1L), ballFrequencyRow(3, 1L),
                ballFrequencyRow(4, 1L), ballFrequencyRow(5, 1L), ballFrequencyRow(6, 1L)
        ));

        WinningStatisticsCacheService cacheService =
                new WinningStatisticsCacheService(winningNumberRepository, summaryRepository, meterRegistry);
        cacheService.frequency();

        long latencyCount = meterRegistry.get("kraft.statistics.frequency.latency")
                .tag("source", "recompute")
                .timer()
                .count();
        assertThat(latencyCount).isEqualTo(1L);
    }

    @Test
    @DisplayName("요약을 재사용하지 못하면 frequency cache miss 메트릭을 기록한다")
    void recordsCacheMissMetricWhenSummaryCannotBeUsed() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        when(winningNumberRepository.findMaxRound()).thenReturn(Optional.of(1201));
        when(summaryRepository.findAllByOrderByBallAsc()).thenReturn(List.of(
                new WinningNumberFrequencySummaryEntity(1, 10L, 1200)
        ));
        when(winningNumberRepository.findBallFrequencies()).thenReturn(List.of(
                ballFrequencyRow(1, 1L), ballFrequencyRow(2, 1L), ballFrequencyRow(3, 1L),
                ballFrequencyRow(4, 1L), ballFrequencyRow(5, 1L), ballFrequencyRow(6, 1L)
        ));

        WinningStatisticsCacheService cacheService =
                new WinningStatisticsCacheService(winningNumberRepository, summaryRepository, meterRegistry);
        cacheService.frequency();

        assertThat(meterRegistry.get("kraft.statistics.frequency.cache.miss")
                .tag("source", "recompute")
                .counter()
                .count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("요약 갱신 지연시간 메트릭을 기록한다")
    void recordsRefreshLatencyMetric() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        when(winningNumberRepository.findMaxRound()).thenReturn(Optional.of(1201));
        when(winningNumberRepository.findBallFrequencies()).thenReturn(List.of(
                ballFrequencyRow(1, 1L), ballFrequencyRow(2, 1L), ballFrequencyRow(3, 1L),
                ballFrequencyRow(4, 1L), ballFrequencyRow(5, 1L), ballFrequencyRow(6, 1L)
        ));

        WinningStatisticsCacheService cacheService =
                new WinningStatisticsCacheService(winningNumberRepository, summaryRepository, meterRegistry);
        cacheService.refreshFrequencySummary();

        assertThat(meterRegistry.get("kraft.statistics.frequency.summary.refresh.latency")
                .timer()
                .count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("combinationPrizeHistory는 1등과 2등을 올바르게 분류하고 번호를 정렬한다")
    void combinationPrizeHistoryClassifiesFirstAndSecond() {
        WinningNumberRepository.PrizeHitWithRankRow first = prizeHitRow(100, LocalDate.of(2020, 1, 1), 1);
        WinningNumberRepository.PrizeHitWithRankRow second = prizeHitRow(200, LocalDate.of(2021, 1, 1), 2);

        when(winningNumberRepository.findPrizeHitsByNumbers(1, 2, 3, 4, 5, 6))
                .thenReturn(List.of(first, second));

        WinningStatisticsCacheService cacheService = new WinningStatisticsCacheService(winningNumberRepository, summaryRepository);
        var result = cacheService.combinationPrizeHistory(List.of(6, 5, 4, 3, 2, 1));

        assertThat(result.numbers()).isEqualTo(List.of(1, 2, 3, 4, 5, 6));
        assertThat(result.firstPrizeCount()).isEqualTo(1);
        assertThat(result.secondPrizeCount()).isEqualTo(1);
        assertThat(result.firstPrizeHits()).hasSize(1);
        assertThat(result.secondPrizeHits()).hasSize(1);
    }

    // ---- WinningStatisticsService 조합 로직 테스트 ----

    @Test
    @DisplayName("frequencySummary는 출현 빈도가 낮은 번호 6개를 정렬된 상태로 반환한다")
    void frequencySummarySelectsLowest6() {
        when(winningNumberRepository.findMaxRound()).thenReturn(Optional.of(100));
        when(winningNumberRepository.count()).thenReturn(100L);

        List<WinningNumberFrequencySummaryEntity> rows = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            rows.add(new WinningNumberFrequencySummaryEntity(i, 1L, 100));
        }
        for (int i = 7; i <= 45; i++) {
            rows.add(new WinningNumberFrequencySummaryEntity(i, 100L, 100));
        }
        when(summaryRepository.findAllByOrderByBallAsc()).thenReturn(rows);
        when(winningNumberRepository.findPrizeHitsByNumbers(1, 2, 3, 4, 5, 6)).thenReturn(List.of());

        WinningStatisticsCacheService cacheService = new WinningStatisticsCacheService(winningNumberRepository, summaryRepository);
        WinningStatisticsService service = new WinningStatisticsService(cacheService);
        var summary = service.frequencySummary();

        assertThat(summary.lowSixCombinationHistory().numbers())
                .containsExactly(1, 2, 3, 4, 5, 6);
    }

    @Test
    @DisplayName("dataChanged=false인 이벤트 수신 시 요약 갱신을 수행하지 않는다")
    void evictCachesOnCollectedDataNotChangedNoSummaryRefresh() {
        WinningStatisticsCacheService mockCacheService = org.mockito.Mockito.mock(WinningStatisticsCacheService.class);
        WinningStatisticsService service = new WinningStatisticsService(mockCacheService);
        WinningNumbersCollectedEvent event = WinningNumbersCollectedEvent.of(0, 0, 0, 0);

        service.evictCachesOnCollected(event);

        verify(mockCacheService, never()).refreshFrequencySummary();
    }

    @Test
    @DisplayName("dataChanged=true인 이벤트 수신 시 통계 캐시를 비우고 요약을 갱신한다")
    void evictCachesOnCollectedDataChangedClearsCachesAndRefreshes() {
        WinningStatisticsCacheService mockCacheService = mock(WinningStatisticsCacheService.class);
        CacheManager cacheManager = mock(CacheManager.class);
        Cache frequency = mock(Cache.class);
        Cache history = mock(Cache.class);
        Cache summary = mock(Cache.class);
        when(cacheManager.getCache("winningNumberFrequency")).thenReturn(frequency);
        when(cacheManager.getCache("combinationPrizeHistory")).thenReturn(history);
        when(cacheManager.getCache("winningFrequencySummary")).thenReturn(summary);

        WinningStatisticsService service = new WinningStatisticsService(mockCacheService, cacheManager);
        WinningNumbersCollectedEvent event = WinningNumbersCollectedEvent.of(1, 0, 0, 0);

        service.evictCachesOnCollected(event);

        verify(frequency).clear();
        verify(history).clear();
        verify(summary).clear();
        verify(mockCacheService).refreshFrequencySummary();
    }

    @Test
    @DisplayName("frequencySummary는 빈도 목록을 포함한다")
    void frequencySummaryContainsFrequencyList() {
        WinningStatisticsCacheService mockCacheService = org.mockito.Mockito.mock(WinningStatisticsCacheService.class);
        List<NumberFrequencyDto> freqList = List.of(new NumberFrequencyDto(1, 5L, 50.0));
        when(mockCacheService.frequency()).thenReturn(freqList);
        when(mockCacheService.combinationPrizeHistory(List.of(1)))
                .thenReturn(new com.kraft.lotto.feature.winningnumber.web.dto.CombinationPrizeHistoryDto(
                        List.of(1), 0, 0, List.of(), List.of()));

        WinningStatisticsService service = new WinningStatisticsService(mockCacheService);
        var summary = service.frequencySummary();

        assertThat(summary.frequencies()).isEqualTo(freqList);
    }

    private static WinningNumberRepository.BallFrequencyRow ballFrequencyRow(int ball, long hitCount) {
        return new WinningNumberRepository.BallFrequencyRow() {
            @Override
            public Integer getBall() { return ball; }
            @Override
            public Long getHitCount() { return hitCount; }
        };
    }

    private static WinningNumberRepository.PrizeHitWithRankRow prizeHitRow(int round, LocalDate drawDate, int prizeRank) {
        return new WinningNumberRepository.PrizeHitWithRankRow() {
            @Override
            public Integer getRound() {
                return round;
            }

            @Override
            public LocalDate getDrawDate() {
                return drawDate;
            }

            @Override
            public Integer getPrizeRank() {
                return prizeRank;
            }
        };
    }
}
