package com.kraft.lotto.feature.winningnumber.application;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.kraft.lotto.feature.winningnumber.domain.WinningStore;
import com.kraft.lotto.feature.winningnumber.event.WinningNumbersCollectedEvent;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningStoreRepository;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import com.kraft.lotto.support.BusinessException;
import com.kraft.lotto.support.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("WinningStoreCollector")
class WinningStoreCollectorTest {

    @Mock WinningStoreApiClient storeApiClient;
    @Mock WinningStoreRepository storeRepository;
    @Mock WinningNumberRepository winningNumberRepository;

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
    @DisplayName("이미 수집된 회차면 재수집하지 않는다")
    void skipsWhenStoresAlreadyCollected() {
        when(winningNumberRepository.findMaxRound()).thenReturn(Optional.of(1226));
        when(storeRepository.existsByRound(1226)).thenReturn(true);

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
    @DisplayName("collectStores: 1등·2등 판매점을 저장한다")
    void collectStoresSavesBothGrades() {
        List<WinningStore> stores1 = List.of(new WinningStore(1226, 1, "상점A", "주소A", 1));
        List<WinningStore> stores2 = List.of(new WinningStore(1226, 2, "상점B", "주소B", 1));
        when(storeApiClient.fetchStores(1226, 1)).thenReturn(stores1);
        when(storeApiClient.fetchStores(1226, 2)).thenReturn(stores2);

        collector.collectStores(1226);

        verify(storeRepository).deleteByRound(1226);
        verify(storeRepository).saveAll(org.mockito.ArgumentMatchers.argThat(
                list -> ((List<?>) list).size() == 2));
    }

    @Test
    @DisplayName("collectStores: 빈 목록이면 기존 데이터 삭제를 시도하지 않는다")
    void persistWhenEntitiesEmptyShouldNotDelete() {
        when(storeApiClient.fetchStores(1226, 1)).thenReturn(List.of());
        when(storeApiClient.fetchStores(1226, 2)).thenReturn(List.of());

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> collector.collectStores(1226))
                .extracting(BusinessException::getErrorCode)
                .isEqualTo(ErrorCode.EXTERNAL_API_FAILURE);

        verify(storeRepository, never()).deleteByRound(1226);
        verify(storeRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("persist: complete=false 배치는 예외를 던진다")
    void persistWhenCompleteFalseShouldThrow() {
        WinningStoreCollector.StoreFetchBatch batch =
                new WinningStoreCollector.StoreFetchBatch(1226, false, List.of());

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> collector.persist(batch))
                .extracting(BusinessException::getErrorCode)
                .isEqualTo(ErrorCode.EXTERNAL_API_FAILURE);

        verify(storeRepository, never()).deleteByRound(1226);
    }
}
