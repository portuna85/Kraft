package com.kraft.lotto.feature.recommend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.kraft.lotto.feature.recommend.domain.PastWinningCache;
import com.kraft.lotto.feature.winningnumber.domain.LottoCombination;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("과거 당첨 번호 캐시 로더")
class PastWinningCacheLoaderTest {

    private static final LottoCombination EXISTING = LottoCombination.of(1, 7, 13, 22, 34, 45);
    private static final LottoCombination RELOADED = LottoCombination.of(2, 8, 14, 23, 35, 44);

    @Mock
    WinningNumberRepository repository;

    @Test
    @DisplayName("성공적인 리로드 시 새로운 스냅샷으로 교체한다")
    void reloadSwapsInNewSnapshot() {
        PastWinningCache cache = new PastWinningCache();
        cache.replace(List.of(EXISTING));
        when(repository.findAllCombinationsOrderByRoundAsc()).thenReturn(List.of(row(RELOADED)));

        new PastWinningCacheLoader(cache, repository).reload();

        assertThat(cache.contains(RELOADED)).isTrue();
        assertThat(cache.contains(EXISTING)).isFalse();
    }

    @Test
    @DisplayName("리로드 실패 시 이전 스냅샷을 유지한다")
    void failedReloadKeepsPreviousSnapshot() {
        PastWinningCache cache = new PastWinningCache();
        cache.replace(List.of(EXISTING));
        when(repository.findAllCombinationsOrderByRoundAsc()).thenThrow(new IllegalStateException("db unavailable"));

        new PastWinningCacheLoader(cache, repository).reload();

        assertThat(cache.contains(EXISTING)).isTrue();
        assertThat(cache.size()).isEqualTo(1);
    }

    private static WinningNumberRepository.CombinationRow row(LottoCombination combination) {
        List<Integer> numbers = combination.numbers();
        return new WinningNumberRepository.CombinationRow() {
            @Override public Integer getN1() { return numbers.get(0); }
            @Override public Integer getN2() { return numbers.get(1); }
            @Override public Integer getN3() { return numbers.get(2); }
            @Override public Integer getN4() { return numbers.get(3); }
            @Override public Integer getN5() { return numbers.get(4); }
            @Override public Integer getN6() { return numbers.get(5); }
        };
    }
}
