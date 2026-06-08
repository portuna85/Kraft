package com.kraft.lotto.feature.statistics.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
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
import org.mockito.InOrder;
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
    @DisplayName("요약 데이터의 공 번호가 범위를 벗어나면 재계산한다")
    void recomputesWhenSummaryHasOutOfRangeBall() {
        when(winningNumberRepository.findMaxRound()).thenReturn(Optional.of(1201));
        when(summaryRepository.findAllByOrderByBallAsc()).thenReturn(invalidSummaryRows(1201, 46, 1L));
        when(winningNumberRepository.findBallFrequencies()).thenReturn(List.of(
                ballFrequencyRow(1, 2L), ballFrequencyRow(2, 2L), ballFrequencyRow(3, 2L),
                ballFrequencyRow(4, 2L), ballFrequencyRow(5, 2L), ballFrequencyRow(6, 2L)
        ));

        WinningStatisticsCacheService cacheService = new WinningStatisticsCacheService(winningNumberRepository, summaryRepository);
        List<NumberFrequencyDto> result = cacheService.frequency();

        assertThat(result).hasSize(45);
        verify(winningNumberRepository).findBallFrequencies();
    }

    @Test
    @DisplayName("요약 데이터의 빈도가 음수면 재계산한다")
    void recomputesWhenSummaryHasNegativeHitCount() {
        when(winningNumberRepository.findMaxRound()).thenReturn(Optional.of(1201));
        when(summaryRepository.findAllByOrderByBallAsc()).thenReturn(invalidSummaryRows(1201, 1, -1L));
        when(winningNumberRepository.findBallFrequencies()).thenReturn(List.of(
                ballFrequencyRow(1, 2L), ballFrequencyRow(2, 2L), ballFrequencyRow(3, 2L),
                ballFrequencyRow(4, 2L), ballFrequencyRow(5, 2L), ballFrequencyRow(6, 2L)
        ));

        WinningStatisticsCacheService cacheService = new WinningStatisticsCacheService(winningNumberRepository, summaryRepository);
        cacheService.frequency();

        verify(winningNumberRepository).findBallFrequencies();
    }

    @Test
    @DisplayName("요약 데이터에 중복 공 번호가 있으면 재계산한다")
    void recomputesWhenSummaryHasDuplicateBall() {
        when(winningNumberRepository.findMaxRound()).thenReturn(Optional.of(1201));
        List<WinningNumberFrequencySummaryEntity> rows = new ArrayList<>();
        rows.add(new WinningNumberFrequencySummaryEntity(1, 1L, 1201));
        rows.add(new WinningNumberFrequencySummaryEntity(1, 1L, 1201));
        for (int i = 3; i <= 45; i++) {
            rows.add(new WinningNumberFrequencySummaryEntity(i, 1L, 1201));
        }
        when(summaryRepository.findAllByOrderByBallAsc()).thenReturn(rows);
        when(winningNumberRepository.findBallFrequencies()).thenReturn(List.of(
                ballFrequencyRow(1, 2L), ballFrequencyRow(2, 2L), ballFrequencyRow(3, 2L),
                ballFrequencyRow(4, 2L), ballFrequencyRow(5, 2L), ballFrequencyRow(6, 2L)
        ));

        WinningStatisticsCacheService cacheService = new WinningStatisticsCacheService(winningNumberRepository, summaryRepository);
        cacheService.frequency();

        verify(winningNumberRepository).findBallFrequencies();
    }

    @Test
    @DisplayName("요약 라운드가 최신 회차와 다르면 재계산한다")
    void recomputesWhenSummaryRoundMismatch() {
        when(winningNumberRepository.findMaxRound()).thenReturn(Optional.of(1201));
        List<WinningNumberFrequencySummaryEntity> rows = new ArrayList<>();
        for (int i = 1; i <= 45; i++) {
            rows.add(new WinningNumberFrequencySummaryEntity(i, 1L, 1200));
        }
        when(summaryRepository.findAllByOrderByBallAsc()).thenReturn(rows);
        when(winningNumberRepository.findBallFrequencies()).thenReturn(List.of(
                ballFrequencyRow(1, 2L), ballFrequencyRow(2, 2L), ballFrequencyRow(3, 2L),
                ballFrequencyRow(4, 2L), ballFrequencyRow(5, 2L), ballFrequencyRow(6, 2L)
        ));

        WinningStatisticsCacheService cacheService = new WinningStatisticsCacheService(winningNumberRepository, summaryRepository);
        cacheService.frequency();

        verify(winningNumberRepository).findBallFrequencies();
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
    @DisplayName("최신 회차가 0이면 요약 저장 없이 종료한다")
    void refreshFrequencySummaryReturnsWhenLatestRoundIsZero() {
        when(winningNumberRepository.findMaxRound()).thenReturn(Optional.of(0));

        WinningStatisticsCacheService cacheService =
                new WinningStatisticsCacheService(winningNumberRepository, summaryRepository, new SimpleMeterRegistry());
        cacheService.refreshFrequencySummary();

        verify(summaryRepository, never()).saveAll(ArgumentMatchers.anyList());
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

        verify(mockCacheService, never()).refreshAll();
    }

    @Test
    @DisplayName("dataChanged=true인 이벤트 수신 시 통계 캐시를 비우고 요약을 갱신한다")
    void evictCachesOnCollectedDataChangedClearsCachesAndRefreshes() {
        WinningStatisticsCacheService mockCacheService = mock(WinningStatisticsCacheService.class);
        when(mockCacheService.refreshAll()).thenReturn(true);
        CacheManager cacheManager = mock(CacheManager.class);
        Cache frequency = mock(Cache.class);
        Cache frequencyPeriod = mock(Cache.class);
        Cache history = mock(Cache.class);
        Cache summary = mock(Cache.class);
        Cache patternStats = mock(Cache.class);
        Cache companionNumbers = mock(Cache.class);
        when(cacheManager.getCache("winningNumberFrequency")).thenReturn(frequency);
        when(cacheManager.getCache("winningNumberFrequencyPeriod")).thenReturn(frequencyPeriod);
        when(cacheManager.getCache("combinationPrizeHistory")).thenReturn(history);
        when(cacheManager.getCache("winningFrequencySummary")).thenReturn(summary);
        when(cacheManager.getCache("patternStats")).thenReturn(patternStats);
        when(cacheManager.getCache("companionNumbers")).thenReturn(companionNumbers);

        WinningStatisticsService service = new WinningStatisticsService(mockCacheService, cacheManager);
        WinningNumbersCollectedEvent event = WinningNumbersCollectedEvent.of(1, 0, 0, 0);

        service.evictCachesOnCollected(event);

        verify(frequency).clear();
        verify(frequencyPeriod).clear();
        verify(history).clear();
        verify(summary).clear();
        verify(patternStats).clear();
        verify(companionNumbers).clear();
        verify(mockCacheService).refreshAll();
    }

    @Test
    @DisplayName("cacheManager가 없으면 캐시 삭제를 건너뛰고 요약만 갱신한다")
    void evictCachesOnCollectedWithoutCacheManager() {
        WinningStatisticsCacheService mockCacheService = mock(WinningStatisticsCacheService.class);
        when(mockCacheService.refreshAll()).thenReturn(true);
        WinningStatisticsService service = new WinningStatisticsService(mockCacheService);
        WinningNumbersCollectedEvent event = WinningNumbersCollectedEvent.of(1, 0, 0, 0);

        service.evictCachesOnCollected(event);

        verify(mockCacheService).refreshAll();
    }

    @Test
    @DisplayName("특정 캐시가 null이면 나머지 캐시만 삭제한다")
    void evictCachesOnCollectedWithPartiallyMissingCaches() {
        WinningStatisticsCacheService mockCacheService = mock(WinningStatisticsCacheService.class);
        when(mockCacheService.refreshAll()).thenReturn(true);
        CacheManager cacheManager = mock(CacheManager.class);
        Cache frequency = mock(Cache.class);
        Cache summary = mock(Cache.class);
        Cache patternStats = mock(Cache.class);
        when(cacheManager.getCache("winningNumberFrequency")).thenReturn(frequency);
        when(cacheManager.getCache("winningNumberFrequencyPeriod")).thenReturn(null);
        when(cacheManager.getCache("combinationPrizeHistory")).thenReturn(null);
        when(cacheManager.getCache("winningFrequencySummary")).thenReturn(summary);
        when(cacheManager.getCache("patternStats")).thenReturn(patternStats);
        when(cacheManager.getCache("companionNumbers")).thenReturn(null);

        WinningStatisticsService service = new WinningStatisticsService(mockCacheService, cacheManager);
        WinningNumbersCollectedEvent event = WinningNumbersCollectedEvent.of(1, 0, 0, 0);

        service.evictCachesOnCollected(event);

        verify(frequency).clear();
        verify(summary).clear();
        verify(patternStats).clear();
        verify(mockCacheService).refreshAll();
    }

    @Test
    @DisplayName("데이터 변경 이벤트 시 요약 테이블 갱신은 캐시 삭제보다 먼저 실행된다")
    void evictCachesOnCollectedRefreshesSummaryBeforeCacheEviction() {
        WinningStatisticsCacheService mockCacheService = mock(WinningStatisticsCacheService.class);
        when(mockCacheService.refreshAll()).thenReturn(true);
        CacheManager cacheManager = mock(CacheManager.class);
        Cache frequency = mock(Cache.class);
        when(cacheManager.getCache("winningNumberFrequency")).thenReturn(frequency);
        when(cacheManager.getCache("winningNumberFrequencyPeriod")).thenReturn(null);
        when(cacheManager.getCache("combinationPrizeHistory")).thenReturn(null);
        when(cacheManager.getCache("winningFrequencySummary")).thenReturn(null);
        when(cacheManager.getCache("patternStats")).thenReturn(null);
        when(cacheManager.getCache("companionNumbers")).thenReturn(null);

        WinningStatisticsService service = new WinningStatisticsService(mockCacheService, cacheManager);
        service.evictCachesOnCollected(WinningNumbersCollectedEvent.of(1, 0, 0, 0));

        InOrder order = inOrder(mockCacheService, frequency);
        order.verify(mockCacheService).refreshAll();
        order.verify(frequency).clear();
    }

    @Test
    @DisplayName("요약 갱신이 실패하면 캐시 무효화를 수행하지 않는다")
    void evictCachesOnCollectedSkipsEvictionWhenRefreshFails() {
        WinningStatisticsCacheService mockCacheService = mock(WinningStatisticsCacheService.class);
        when(mockCacheService.refreshAll()).thenReturn(false);
        CacheManager cacheManager = mock(CacheManager.class);

        WinningStatisticsService service = new WinningStatisticsService(mockCacheService, cacheManager);
        service.evictCachesOnCollected(WinningNumbersCollectedEvent.of(1, 0, 0, 0));

        verify(mockCacheService).refreshAll();
        verify(cacheManager, never()).getCache(org.mockito.ArgumentMatchers.anyString());
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

    @Test
    @DisplayName("서비스 frequency는 캐시 서비스 결과를 그대로 반환한다")
    void frequencyDelegatesToCacheService() {
        WinningStatisticsCacheService mockCacheService = mock(WinningStatisticsCacheService.class);
        List<NumberFrequencyDto> frequencies = List.of(new NumberFrequencyDto(1, 1L, 1.0));
        when(mockCacheService.frequency()).thenReturn(frequencies);
        WinningStatisticsService service = new WinningStatisticsService(mockCacheService);

        assertThat(service.frequency()).isEqualTo(frequencies);
    }

    @Test
    @DisplayName("서비스 combinationPrizeHistory는 캐시 서비스 결과를 그대로 반환한다")
    void combinationPrizeHistoryDelegatesToCacheService() {
        WinningStatisticsCacheService mockCacheService = mock(WinningStatisticsCacheService.class);
        var history = new com.kraft.lotto.feature.winningnumber.web.dto.CombinationPrizeHistoryDto(
                List.of(1, 2, 3, 4, 5, 6), 0, 0, List.of(), List.of()
        );
        when(mockCacheService.combinationPrizeHistory(List.of(1, 2, 3, 4, 5, 6))).thenReturn(history);
        WinningStatisticsService service = new WinningStatisticsService(mockCacheService);

        assertThat(service.combinationPrizeHistory(List.of(1, 2, 3, 4, 5, 6))).isEqualTo(history);
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

    private static List<WinningNumberFrequencySummaryEntity> invalidSummaryRows(int latestRound, int invalidBall, long invalidCount) {
        List<WinningNumberFrequencySummaryEntity> rows = new ArrayList<>();
        for (int i = 1; i <= 45; i++) {
            if (i == 1) {
                rows.add(new WinningNumberFrequencySummaryEntity(invalidBall, invalidCount, latestRound));
            } else {
                rows.add(new WinningNumberFrequencySummaryEntity(i, 1L, latestRound));
            }
        }
        return rows;
    }
}
