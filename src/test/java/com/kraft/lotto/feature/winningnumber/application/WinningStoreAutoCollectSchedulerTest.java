package com.kraft.lotto.feature.winningnumber.application;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningStoreRepository;
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

    WinningStoreAutoCollectScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new WinningStoreAutoCollectScheduler(storeCollector, winningNumberRepository, storeRepository);
    }

    @Test
    @DisplayName("DB에 회차가 없으면 수집을 건너뛴다")
    void skipsWhenNoRoundsInDb() {
        when(winningNumberRepository.findMaxRound()).thenReturn(Optional.empty());

        scheduler.collectStoreSunday0600();

        verify(storeCollector, never()).collectStores(0);
    }

    @Test
    @DisplayName("1등·2등 판매점이 모두 수집된 경우 스킵한다")
    void skipsWhenStoresAlreadyComplete() {
        when(winningNumberRepository.findMaxRound()).thenReturn(Optional.of(1177));
        when(storeRepository.existsByRoundAndGrade(1177, 1)).thenReturn(true);
        when(storeRepository.existsByRoundAndGrade(1177, 2)).thenReturn(true);

        scheduler.collectStoreSunday0600();

        verify(storeCollector, never()).collectStores(1177);
    }

    @Test
    @DisplayName("1등 판매점이 없으면 수집을 실행한다")
    void collectsWhenGrade1Missing() {
        when(winningNumberRepository.findMaxRound()).thenReturn(Optional.of(1177));
        when(storeRepository.existsByRoundAndGrade(1177, 1)).thenReturn(false);
        when(storeCollector.collectStores(1177)).thenReturn(true);

        scheduler.collectStoreSunday0600();

        verify(storeCollector).collectStores(1177);
    }

    @Test
    @DisplayName("2등 판매점이 없으면 수집을 실행한다")
    void collectsWhenGrade2Missing() {
        when(winningNumberRepository.findMaxRound()).thenReturn(Optional.of(1177));
        when(storeRepository.existsByRoundAndGrade(1177, 1)).thenReturn(true);
        when(storeRepository.existsByRoundAndGrade(1177, 2)).thenReturn(false);
        when(storeCollector.collectStores(1177)).thenReturn(false);

        scheduler.collectStoreSunday0600();

        verify(storeCollector).collectStores(1177);
    }
}
