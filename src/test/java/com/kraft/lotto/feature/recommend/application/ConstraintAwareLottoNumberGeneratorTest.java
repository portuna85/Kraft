package com.kraft.lotto.feature.recommend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kraft.lotto.feature.winningnumber.domain.LottoCombination;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ConstraintAwareLottoNumberGenerator")
class ConstraintAwareLottoNumberGeneratorTest {

    @Test
    @DisplayName("invalid threshold values fail fast")
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
    @DisplayName("non-positive attempt bounds fail fast")
    void rejectsNonPositiveAttemptBounds() {
        assertThatThrownBy(() -> new ConstraintAwareLottoNumberGenerator(31, 4, 3, 0, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("initialPickMaxAttempts");
        assertThatThrownBy(() -> new ConstraintAwareLottoNumberGenerator(31, 4, 3, 100, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fixupMaxAttempts");
    }

    @Test
    @DisplayName("generated combination has six unique sorted numbers")
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
    @DisplayName("combination includes at least one number above birthday threshold")
    void birthdayThresholdConstraintSatisfied() {
        int threshold = 31;
        var generator = new ConstraintAwareLottoNumberGenerator(threshold, 4, 3);

        for (int i = 0; i < 50; i++) {
            List<Integer> numbers = generator.generate().numbers();
            assertThat(numbers).anyMatch(n -> n > threshold);
        }
    }

    @Test
    @DisplayName("fixup removes long consecutive runs")
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
    @DisplayName("decade threshold is respected")
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