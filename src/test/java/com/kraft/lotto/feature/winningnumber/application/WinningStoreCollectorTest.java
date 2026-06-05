package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kraft.lotto.feature.winningnumber.domain.WinningStore;
import com.kraft.lotto.feature.winningnumber.event.WinningNumbersCollectedEvent;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningStoreRepository;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("WinningStoreCollector")
class WinningStoreCollectorTest {

    @Mock WinningStoreApiClient storeApiClient;
    @Mock WinningStoreRepository storeRepository;
    @Mock WinningNumberRepository winningNumberRepository;
    @Spy  Clock clock = Clock.fixed(Instant.parse("2026-06-05T12:00:00Z"), ZoneId.of("Asia/Seoul"));

    @InjectMocks
    WinningStoreCollector collector;

    @Test
    @DisplayName("dataChanged=false 이면 수집을 건너뛴다")
    void skipsWhenDataNotChanged() {
        WinningNumbersCollectedEvent event = WinningNumbersCollectedEvent.of(0, 0, 1, 0);

        collector.onCollected(event);

        verify(winningNumberRepository, never()).findMaxRound();
    }

    @Test
    @DisplayName("1등·2등 모두 수집된 회차면 재수집하지 않는다")
    void skipsWhenAllGradesAlreadyCollected() {
        when(winningNumberRepository.findMaxRound()).thenReturn(Optional.of(1226));
        when(storeRepository.existsByRoundAndGrade(1226, 1)).thenReturn(true);
        when(storeRepository.existsByRoundAndGrade(1226, 2)).thenReturn(true);

        collector.onCollected(WinningNumbersCollectedEvent.of(1, 0, 0, 0));

        verify(storeApiClient, never()).fetchStores(anyInt(), anyInt());
    }

    @Test
    @DisplayName("최신 회차가 없으면 수집을 건너뛴다")
    void skipsWhenNoLatestRound() {
        when(winningNumberRepository.findMaxRound()).thenReturn(Optional.empty());

        collector.onCollected(WinningNumbersCollectedEvent.of(1, 0, 0, 0));

        verify(storeApiClient, never()).fetchStores(anyInt(), anyInt());
    }

    @Test
    @DisplayName("collectStores: 1등·2등 판매점을 grade별로 독립 저장한다")
    void collectStoresSavesBothGradesIndependently() {
        List<WinningStore> stores1 = List.of(new WinningStore(1226, 1, "상점A", "주소A", 1));
        List<WinningStore> stores2 = List.of(new WinningStore(1226, 2, "상점B", "주소B", 1));
        when(storeApiClient.fetchStores(1226, 1)).thenReturn(stores1);
        when(storeApiClient.fetchStores(1226, 2)).thenReturn(stores2);

        boolean result = collector.collectStores(1226);

        assertThat(result).isTrue();
        verify(storeRepository).deleteByRoundAndGrade(1226, 1);
        verify(storeRepository).deleteByRoundAndGrade(1226, 2);
        verify(storeRepository, org.mockito.Mockito.times(2))
                .saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("collectStores: 한 등급 실패 시 false를 반환하지만 성공 등급은 저장된다")
    void collectStoresReturnsFalseOnPartialFailure() {
        List<WinningStore> stores1 = List.of(new WinningStore(1226, 1, "상점A", "주소A", 1));
        when(storeApiClient.fetchStores(1226, 1)).thenReturn(stores1);
        when(storeApiClient.fetchStores(1226, 2)).thenReturn(List.of());

        boolean result = collector.collectStores(1226);

        assertThat(result).isFalse();
        verify(storeRepository).deleteByRoundAndGrade(1226, 1);
        verify(storeRepository, org.mockito.Mockito.times(1))
                .saveAll(org.mockito.ArgumentMatchers.anyList());
        verify(storeRepository, never()).deleteByRoundAndGrade(1226, 2);
    }

    @Test
    @DisplayName("collectStores: 모든 등급 실패 시 false를 반환하고 저장소를 건드리지 않는다")
    void collectStoresReturnsFalseWhenAllGradesFail() {
        when(storeApiClient.fetchStores(1226, 1)).thenReturn(List.of());
        when(storeApiClient.fetchStores(1226, 2)).thenReturn(List.of());

        boolean result = collector.collectStores(1226);

        assertThat(result).isFalse();
        verify(storeRepository, never()).deleteByRoundAndGrade(anyInt(), anyInt());
        verify(storeRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }
}
