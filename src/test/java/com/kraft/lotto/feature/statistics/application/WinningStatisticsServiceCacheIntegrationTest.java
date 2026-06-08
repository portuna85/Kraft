package com.kraft.lotto.feature.statistics.application;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberStatisticsRepository.BallFrequencyRow;
import com.kraft.lotto.support.BusinessException;
import com.kraft.lotto.support.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = WinningStatisticsServiceCacheIntegrationTest.CacheTestConfig.class)
@DisplayName("당첨 통계 서비스 캐시 통합 테스트")
class WinningStatisticsServiceCacheIntegrationTest {

    @Configuration
    @EnableCaching
    static class CacheTestConfig {

        @Bean
        WinningStatisticsCacheService winningStatisticsCacheService(WinningNumberRepository repository) {
            return new WinningStatisticsCacheService(repository, null);
        }

        @Bean
        CombinationPrizeHistoryKeyGenerator combinationPrizeHistoryKeyGenerator() {
            return new CombinationPrizeHistoryKeyGenerator();
        }

        @Bean
        WinningStatisticsService winningStatisticsService(WinningStatisticsCacheService cacheService) {
            return new WinningStatisticsService(cacheService);
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                    "winningNumberFrequency",
                    "combinationPrizeHistory",
                    "winningFrequencySummary"
            );
        }
    }

    @Autowired
    WinningStatisticsService service;

    @MockitoBean
    WinningNumberRepository repository;

    @Test
    @DisplayName("빈도 요약이 프록시를 통해 캐시를 사용하여 중복 빈도 집계 쿼리를 방지한다")
    void frequencySummaryUsesCachedFrequencyViaProxy() {
        when(repository.findBallFrequencies()).thenReturn(List.of(
                ballFrequencyRow(1, 1L), ballFrequencyRow(2, 1L), ballFrequencyRow(3, 1L),
                ballFrequencyRow(4, 1L), ballFrequencyRow(5, 1L), ballFrequencyRow(6, 1L)
        ));
        when(repository.findPrizeHitsByNumbers(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(repository.findMaxRound()).thenReturn(Optional.of(1));

        service.frequencySummary();
        service.frequencySummary();

        verify(repository, times(1)).findBallFrequencies();
    }

    @Test
    @DisplayName("유효하지 않은 번호 조합은 캐시 키 평가 전에 실패한다")
    void invalidCombinationInputsFailBeforeRepositoryQuery() {
        assertInvalidCombinationRejected(null);
        assertInvalidCombinationRejected(List.of(1, 2, 3, 4, 5));
        assertInvalidCombinationRejected(List.of(1, 2, 3, 4, 5, 5));
        assertInvalidCombinationRejected(List.of(1, 2, 3, 4, 5, 46));

        verify(repository, never()).findPrizeHitsByNumbers(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
    }

    private static BallFrequencyRow ballFrequencyRow(int ball, long hitCount) {
        return new BallFrequencyRow() {
            @Override public Integer getBall() { return ball; }
            @Override public Long getHitCount() { return hitCount; }
        };
    }

    private void assertInvalidCombinationRejected(List<Integer> numbers) {
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> service.combinationPrizeHistory(numbers))
                .extracting(BusinessException::getErrorCode)
                .isEqualTo(ErrorCode.LOTTO_INVALID_NUMBER);
    }
}
