package com.kraft.lotto.feature.winningnumber.application;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberEntity;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningStoreRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("WinningStoreAutoCollectScheduler")
class WinningStoreAutoCollectSchedulerTest {

    @Mock WinningStoreCollector storeCollector;
    @Mock WinningNumberRepository winningNumberRepository;
    @Mock WinningStoreRepository storeRepository;
    @Mock LottoCollectionCommandService collectionService;

    WinningStoreAutoCollectScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new WinningStoreAutoCollectScheduler(storeCollector, winningNumberRepository, storeRepository, collectionService);
    }

    @Test
    @DisplayName("DB에 회차가 없으면 수집을 건너뛴다")
    void skipsWhenNoRoundsInDb() {
        when(winningNumberRepository.findTopByOrderByRoundDesc()).thenReturn(Optional.empty());

        scheduler.collectStoreSunday0600();

        verify(storeCollector, never()).collectStores(0);
    }

    @Test
    @DisplayName("1등·2등 판매점이 모두 수집된 경우 스킵한다")
    void skipsWhenStoresAlreadyComplete() {
        when(winningNumberRepository.findTopByOrderByRoundDesc()).thenReturn(Optional.of(entityWithSecondPrize(1177, 50_000_000L)));
        when(storeRepository.existsByRoundAndGrade(1177, 1)).thenReturn(true);
        when(storeRepository.existsByRoundAndGrade(1177, 2)).thenReturn(true);

        scheduler.collectStoreSunday0600();

        verify(storeCollector, never()).collectStores(1177);
    }

    @Test
    @DisplayName("1등 판매점이 없으면 수집을 실행한다")
    void collectsWhenGrade1Missing() {
        when(winningNumberRepository.findTopByOrderByRoundDesc()).thenReturn(Optional.of(entityWithSecondPrize(1177, 50_000_000L)));
        when(storeRepository.existsByRoundAndGrade(1177, 1)).thenReturn(false);
        when(storeCollector.collectStores(1177)).thenReturn(true);

        scheduler.collectStoreSunday0600();

        verify(storeCollector).collectStores(1177);
    }

    @Test
    @DisplayName("2등 판매점이 없으면 수집을 실행한다")
    void collectsWhenGrade2Missing() {
        when(winningNumberRepository.findTopByOrderByRoundDesc()).thenReturn(Optional.of(entityWithSecondPrize(1177, 50_000_000L)));
        when(storeRepository.existsByRoundAndGrade(1177, 1)).thenReturn(true);
        when(storeRepository.existsByRoundAndGrade(1177, 2)).thenReturn(false);
        when(storeCollector.collectStores(1177)).thenReturn(false);

        scheduler.collectStoreSunday0600();

        verify(storeCollector).collectStores(1177);
    }

    @Test
    @DisplayName("secondPrize=0이면 해당 회차를 refresh 재수집한다")
    void refreshesWhenSecondPrizeIsZero() {
        when(winningNumberRepository.findTopByOrderByRoundDesc()).thenReturn(Optional.of(entityWithSecondPrize(1177, 0L)));
        when(storeRepository.existsByRoundAndGrade(1177, 1)).thenReturn(false);
        when(storeCollector.collectStores(1177)).thenReturn(true);

        scheduler.collectStoreSunday0600();

        verify(collectionService).collectOneRefresh(1177);
        verify(storeCollector).collectStores(1177);
    }

    @Test
    @DisplayName("secondPrize>0이면 refresh를 호출하지 않는다")
    void doesNotRefreshWhenSecondPrizeIsPositive() {
        when(winningNumberRepository.findTopByOrderByRoundDesc()).thenReturn(Optional.of(entityWithSecondPrize(1177, 50_000_000L)));
        when(storeRepository.existsByRoundAndGrade(1177, 1)).thenReturn(true);
        when(storeRepository.existsByRoundAndGrade(1177, 2)).thenReturn(true);

        scheduler.collectStoreSunday0600();

        verify(collectionService, never()).collectOneRefresh(1177);
    }

    private static WinningNumberEntity entityWithSecondPrize(int round, long secondPrize) {
        LocalDate date = LocalDate.of(2025, 6, 1);
        LocalDateTime now = LocalDateTime.of(2025, 6, 1, 22, 0);
        return new WinningNumberEntity(
                round, date,
                1, 2, 3, 4, 5, 6, 7,
                1_000_000_000L, 5,
                50_000_000_000L,
                0L,
                secondPrize, secondPrize > 0 ? 100 : 0,
                null, now, now, now
        );
    }
}
