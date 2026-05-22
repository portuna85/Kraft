package com.kraft.lotto.feature.recommend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kraft.lotto.feature.winningnumber.domain.LottoCombination;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("제약 조건 인식 로또 번호 생성기 테스트")
class ConstraintAwareLottoNumberGeneratorTest {

    @Test
    @DisplayName("불가능한 제약 조건일 경우 타임아웃 예외가 발생한다")
    void throwsTimeoutWhenConstraintsAreImpossible() {
        var generator = new ConstraintAwareLottoNumberGenerator(
                45,
                2,
                1
        );

        assertThatThrownBy(generator::generate)
                .isInstanceOf(RecommendGenerationTimeoutException.class);
    }

    @Test
    @DisplayName("생성된 조합은 6개의 고유하고 정렬된 1~45 번호를 포함한다")
    void generatedCombinationHasSixUniqueSortedNumbers() {
        var generator = new ConstraintAwareLottoNumberGenerator(31, 4, 3);

        LottoCombination combo = generator.generate();
        List<Integer> numbers = combo.numbers();

        assertThat(numbers).hasSize(6);
        assertThat(numbers).doesNotHaveDuplicates();
        assertThat(numbers).isSorted();
        assertThat(numbers).allMatch(n -> n >= 1 && n <= 45);
    }

    @Test
    @DisplayName("birthdayThreshold 초과 번호가 반드시 1개 이상 포함된다")
    void birthdayThresholdConstraintSatisfied() {
        int threshold = 31;
        var generator = new ConstraintAwareLottoNumberGenerator(threshold, 4, 3);

        for (int i = 0; i < 50; i++) {
            List<Integer> numbers = generator.generate().numbers();
            assertThat(numbers).anyMatch(n -> n > threshold);
        }
    }

    @Test
    @DisplayName("연속 번호 fixup이 동작하여 3개 이상 연속 번호가 없다")
    void consecutiveRunFixupWorks() {
        var generator = new ConstraintAwareLottoNumberGenerator(31, 3, 3);

        for (int i = 0; i < 50; i++) {
            List<Integer> numbers = generator.generate().numbers();
            for (int j = 1; j < numbers.size() - 1; j++) {
                boolean threeConsecutive = numbers.get(j) - numbers.get(j - 1) == 1
                        && numbers.get(j + 1) - numbers.get(j) == 1;
                assertThat(threeConsecutive)
                        .as("연속 3개 발견: %s", numbers)
                        .isFalse();
            }
        }
    }

    @Test
    @DisplayName("decadeThreshold=3일 때 각 십단위 구간에 최대 2개 이하의 번호가 선택된다")
    void decadeBucketDistributionRespected() {
        var generator = new ConstraintAwareLottoNumberGenerator(31, 4, 3);

        for (int i = 0; i < 50; i++) {
            List<Integer> numbers = generator.generate().numbers();
            int[] buckets = new int[5];
            for (int n : numbers) {
                int b = n <= 9 ? 0 : n <= 19 ? 1 : n <= 29 ? 2 : n <= 39 ? 3 : 4;
                buckets[b]++;
            }
            for (int count : buckets) {
                assertThat(count).as("십단위 구간 초과: %s", numbers).isLessThanOrEqualTo(2);
            }
        }
    }
}
