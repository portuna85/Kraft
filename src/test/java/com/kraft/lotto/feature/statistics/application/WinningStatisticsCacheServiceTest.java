package com.kraft.lotto.feature.statistics.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("통계 캐시 서비스")
class WinningStatisticsCacheServiceTest {

    @Mock
    WinningNumberRepository repository;

    @Test
    @DisplayName("동반 번호 동률은 같은 순위를 갖는다")
    void companionNumbersTiedHitCountShouldHaveSameRank() {
        when(repository.findCompanionNumbers(7)).thenReturn(List.of(
                companionRow(1, 10L),
                companionRow(2, 10L),
                companionRow(3, 8L),
                companionRow(4, 8L),
                companionRow(5, 3L)
        ));

        WinningStatisticsCacheService service = WinningStatisticsCacheServiceBuilder.forRepository(repository).build();
        var result = service.companionNumbers(7);

        assertThat(result).extracting(dto -> dto.rank())
                .containsExactly(1, 1, 2, 2, 3);
    }

    private static WinningNumberRepository.CompanionRow companionRow(int otherBall, long hitCount) {
        return new WinningNumberRepository.CompanionRow() {
            @Override
            public Integer getOtherBall() {
                return otherBall;
            }

            @Override
            public Long getHitCount() {
                return hitCount;
            }
        };
    }
}
