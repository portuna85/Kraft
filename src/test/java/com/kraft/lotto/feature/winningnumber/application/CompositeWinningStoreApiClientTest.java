package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kraft.lotto.feature.winningnumber.domain.WinningStore;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CompositeWinningStoreApiClient")
class CompositeWinningStoreApiClientTest {

    private WinningStoreApiClient primary;
    private WinningStoreApiClient fallback;
    private SimpleMeterRegistry meterRegistry;
    private CompositeWinningStoreApiClient composite;

    private static final WinningStore STORE = WinningStore.withSource(
            1230, 1, "행운복권방", "서울", 1, "dhlottery");

    @BeforeEach
    void setUp() {
        primary      = mock(WinningStoreApiClient.class);
        fallback     = mock(WinningStoreApiClient.class);
        meterRegistry = new SimpleMeterRegistry();
        composite = new CompositeWinningStoreApiClient(
                primary, "dhlottery", fallback, "public-data", meterRegistry);
    }

    @Test
    @DisplayName("primary가 결과를 반환하면 fallback을 호출하지 않는다")
    void returnsPrimaryResultWithoutCallingFallback() {
        when(primary.fetchStores(1230, 1)).thenReturn(List.of(STORE));

        List<WinningStore> result = composite.fetchStores(1230, 1);

        assertThat(result).containsExactly(STORE);
        verify(fallback, never()).fetchStores(1230, 1);
    }

    @Test
    @DisplayName("primary가 빈 List를 반환하면 fallback을 호출한다")
    void callsFallbackWhenPrimaryReturnsEmpty() {
        WinningStore fallbackStore = WinningStore.withSource(1230, 1, "공공데이터방", "경기도", 1, "public-data");
        when(primary.fetchStores(1230, 1)).thenReturn(List.of());
        when(fallback.fetchStores(1230, 1)).thenReturn(List.of(fallbackStore));

        List<WinningStore> result = composite.fetchStores(1230, 1);

        assertThat(result).containsExactly(fallbackStore);
        assertThat(meterRegistry.counter("kraft.store.fallback.used",
                "from", "dhlottery", "to", "public-data", "grade", "1").count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("primary가 예외를 던지면 fallback을 호출한다")
    void callsFallbackWhenPrimaryThrows() {
        WinningStore fallbackStore = WinningStore.withSource(1230, 1, "공공데이터방", "경기도", 1, "public-data");
        when(primary.fetchStores(1230, 1)).thenThrow(new RuntimeException("connection failed"));
        when(fallback.fetchStores(1230, 1)).thenReturn(List.of(fallbackStore));

        List<WinningStore> result = composite.fetchStores(1230, 1);

        assertThat(result).containsExactly(fallbackStore);
        assertThat(meterRegistry.counter("kraft.store.fallback.used",
                "from", "dhlottery", "to", "public-data", "grade", "1").count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("primary 빈 + fallback도 빈이면 빈 List를 반환한다")
    void returnsEmptyWhenBothReturnEmpty() {
        when(primary.fetchStores(1230, 1)).thenReturn(List.of());
        when(fallback.fetchStores(1230, 1)).thenReturn(List.of());

        List<WinningStore> result = composite.fetchStores(1230, 1);

        assertThat(result).isEmpty();
        assertThat(meterRegistry.counter("kraft.store.fallback.exhausted", "grade", "1").count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("primary 예외 + fallback도 예외이면 빈 List를 반환한다 (예외 전파 없음)")
    void returnsEmptyWhenBothThrow() {
        when(primary.fetchStores(1230, 1)).thenThrow(new RuntimeException("primary failed"));
        when(fallback.fetchStores(1230, 1)).thenThrow(new RuntimeException("fallback failed"));

        List<WinningStore> result = composite.fetchStores(1230, 1);

        assertThat(result).isEmpty();
        assertThat(meterRegistry.counter("kraft.store.fallback.exhausted", "grade", "1").count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("primary 예외 + fallback 빈이면 빈 List를 반환한다")
    void returnsEmptyWhenPrimaryThrowsAndFallbackEmpty() {
        when(primary.fetchStores(1230, 1)).thenThrow(new RuntimeException("primary failed"));
        when(fallback.fetchStores(1230, 1)).thenReturn(List.of());

        List<WinningStore> result = composite.fetchStores(1230, 1);

        assertThat(result).isEmpty();
        assertThat(meterRegistry.counter("kraft.store.fallback.exhausted", "grade", "1").count()).isEqualTo(1.0);
    }
}
