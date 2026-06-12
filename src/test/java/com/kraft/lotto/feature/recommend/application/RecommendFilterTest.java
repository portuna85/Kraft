package com.kraft.lotto.feature.recommend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kraft.lotto.support.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("번호 추천 필터")
class RecommendFilterTest {

    @Test
    @DisplayName("조건 없는 필터는 NONE과 동일하다")
    void emptyFilterIsNone() {
        assertThat(RecommendFilter.of(null, null, null).isNone()).isTrue();
    }

    @ParameterizedTest
    @DisplayName("oddCount별 달성 가능한 합산 경계 범위는 유효하다")
    @CsvSource({
        "0, 42,  234",
        "1, 31,  235",
        "2, 24,  244",
        "3, 21,  249",
        "4, 22,  250",
        "5, 27,  249",
        "6, 36,  240",
    })
    void boundaryRangesAreValid(int oddCount, int minSum, int maxSum) {
        assertThat(RecommendFilter.of(oddCount, minSum, maxSum)).isNotNull();
    }

    @ParameterizedTest
    @DisplayName("oddCount + sumRange 조합이 불가능한 경우 예외를 던진다")
    @CsvSource({
        "6, 241, 255",  // oddCount=6 최대합 240 초과
        "0, 21,  41",   // oddCount=0 최소합 42 미만
        "3, 250, 255",  // oddCount=3 최대합 249 초과
        "6, 21,  35",   // oddCount=6 최소합 36 미만
    })
    void infeasibleOddCountSumCombinationThrows(int oddCount, int sumMin, int sumMax) {
        assertThatThrownBy(() -> RecommendFilter.of(oddCount, sumMin, sumMax))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("달성 가능한 합산 범위");
    }

    @Test
    @DisplayName("sumMin만 지정된 경우 oddCount와 교차 검증한다")
    void sumMinOnlyValidatedAgainstOddCount() {
        // oddCount=6 최대합=240, sumMin=241 이면 불가
        assertThatThrownBy(() -> RecommendFilter.of(6, 241, null))
                .isInstanceOf(BusinessException.class);
        // sumMin=240 이면 가능
        assertThat(RecommendFilter.of(6, 240, null)).isNotNull();
    }

    @Test
    @DisplayName("sumMax만 지정된 경우 oddCount와 교차 검증한다")
    void sumMaxOnlyValidatedAgainstOddCount() {
        // oddCount=6 최소합=36, sumMax=35 이면 불가
        assertThatThrownBy(() -> RecommendFilter.of(6, null, 35))
                .isInstanceOf(BusinessException.class);
        // sumMax=36 이면 가능
        assertThat(RecommendFilter.of(6, null, 36)).isNotNull();
    }

    @Test
    @DisplayName("oddCount만 지정하고 sumRange 없으면 검증을 건너뛴다")
    void oddCountAloneDoesNotValidateSumRange() {
        assertThat(RecommendFilter.of(3, null, null)).isNotNull();
    }

    @Test
    @DisplayName("sumMin > sumMax 이면 예외를 던진다")
    void sumMinGreaterThanSumMaxThrows() {
        assertThatThrownBy(() -> RecommendFilter.of(null, 200, 100))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("sumMin must be less than or equal to sumMax");
    }
}
