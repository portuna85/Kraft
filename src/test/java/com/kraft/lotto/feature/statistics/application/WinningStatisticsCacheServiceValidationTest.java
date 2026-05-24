package com.kraft.lotto.feature.statistics.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.kraft.lotto.support.BusinessException;
import com.kraft.lotto.support.ErrorCode;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("통계 캐시 서비스 조합 검증 테스트")
class WinningStatisticsCacheServiceValidationTest {

    @Test
    @DisplayName("유효한 6개 숫자는 정렬된 캐시 키를 반환한다")
    void returnsSortedCacheKeyForValidNumbers() {
        String key = WinningStatisticsCacheService.combinationPrizeHistoryCacheKey(
                List.of(6, 2, 5, 1, 4, 3)
        );

        assertThat(key).isEqualTo("1-2-3-4-5-6");
    }

    @Test
    @DisplayName("숫자 개수가 6개가 아니면 예외가 발생한다")
    void rejectsInvalidSize() {
        assertInvalid(List.of(1, 2, 3, 4, 5));
        assertInvalid(List.of(1, 2, 3, 4, 5, 6, 7));
    }

    @Test
    @DisplayName("null 또는 범위 밖 숫자가 있으면 예외가 발생한다")
    void rejectsNullOrOutOfRangeNumbers() {
        assertInvalid(Arrays.asList(1, 2, 3, 4, 5, null));
        assertInvalid(List.of(0, 2, 3, 4, 5, 6));
        assertInvalid(List.of(1, 2, 3, 4, 5, 46));
    }

    @Test
    @DisplayName("중복 숫자가 있으면 예외가 발생한다")
    void rejectsDuplicateNumbers() {
        assertInvalid(List.of(1, 2, 3, 4, 5, 5));
    }

    private static void assertInvalid(List<Integer> numbers) {
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> WinningStatisticsCacheService.combinationPrizeHistoryCacheKey(numbers))
                .extracting(BusinessException::getErrorCode)
                .isEqualTo(ErrorCode.LOTTO_INVALID_NUMBER);
    }
}
