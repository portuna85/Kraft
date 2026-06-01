package com.kraft.lotto.feature.recommend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kraft.lotto.feature.winningnumber.domain.LottoCombination;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("제약 조건 인식 로또 번호 생성기")
class ConstraintAwareLottoNumberGeneratorTest {

    @Test
    @DisplayName("유효하지 않은 임계값 설정 시 예외가 발생한다")
    void rejectsInvalidThresholds() {
        assertThatThrownBy(() -> new ConstraintAwareLottoNumberGenerator(0, 4, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("birthdayThreshold");
        assertThatThrownBy(() -> new ConstraintAwareLottoNumberGenerator(31, 1, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("longRunThreshold");
        assertThatThrownBy(() -> new ConstraintAwareLottoNumberGenerator(31, 4, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("decadeThreshold");
    }

    @Test
    @DisplayName("양수가 아닌 시도 횟수 설정 시 예외가 발생한다")
    void rejectsNonPositiveAttemptBounds() {
        assertThatThrownBy(() -> new ConstraintAwareLottoNumberGenerator(31, 4, 3, 0, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("initialPickMaxAttempts");
        assertThatThrownBy(() -> new ConstraintAwareLottoNumberGenerator(31, 4, 3, 100, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fixupMaxAttempts");
    }

    @Test
    @DisplayName("생성된 조합은 6개의 유일하고 정렬된 숫자를 가진다")
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
    @DisplayName("생성된 조합은 적어도 하나의 숫자가 생일 임계값보다 크다")
    void birthdayThresholdConstraintSatisfied() {
        int threshold = 31;
        var generator = new ConstraintAwareLottoNumberGenerator(threshold, 4, 3);

        for (int i = 0; i < 50; i++) {
            List<Integer> numbers = generator.generate().numbers();
            assertThat(numbers).anyMatch(n -> n > threshold);
        }
    }

    @Test
    @DisplayName("연속 번호 수정을 통해 긴 연속 번호를 제거한다")
    void consecutiveRunFixupWorks() {
        var generator = new ConstraintAwareLottoNumberGenerator(31, 3, 3);

        for (int i = 0; i < 50; i++) {
            List<Integer> numbers = generator.generate().numbers();
            for (int j = 1; j < numbers.size() - 1; j++) {
                boolean threeConsecutive = numbers.get(j) - numbers.get(j - 1) == 1
                        && numbers.get(j + 1) - numbers.get(j) == 1;
                assertThat(threeConsecutive)
                        .as("found 3-consecutive run: %s", numbers)
                        .isFalse();
            }
        }
    }

    @Test
    @DisplayName("십단위 분포 제약 조건을 준수한다")
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
                assertThat(count).as("decade bucket overflow: %s", numbers).isLessThanOrEqualTo(2);
            }
        }
    }
}
