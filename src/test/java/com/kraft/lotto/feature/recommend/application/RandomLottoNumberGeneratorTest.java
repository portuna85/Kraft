package com.kraft.lotto.feature.recommend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("랜덤 로또 번호 생성기")
class RandomLottoNumberGeneratorTest {

    @Test
    @DisplayName("고정된 시드값으로 결정론적인 조합을 생성한다")
    void deterministicWithFixedSeed() {
        LottoNumberGenerator generatorA = new RandomLottoNumberGenerator(new Random(1234L));
        LottoNumberGenerator generatorB = new RandomLottoNumberGenerator(new Random(1234L));

        assertThat(generatorA.generate()).isEqualTo(generatorB.generate());
        assertThat(generatorA.generate()).isEqualTo(generatorB.generate());
    }

    @Test
    @DisplayName("생성된 조합은 항상 로또의 불변 조건을 만족한다")
    void generatedCombinationHasValidShape() {
        LottoNumberGenerator generator = new RandomLottoNumberGenerator(new Random(7L));

        var combination = generator.generate();

        assertThat(combination.numbers()).hasSize(6);
        assertThat(combination.numbers()).isSorted();
        assertThat(combination.numbers()).doesNotHaveDuplicates();
        assertThat(combination.numbers()).allMatch(n -> n >= 1 && n <= 45);
    }

    @Test
    @DisplayName("null인 Random 객체는 거절한다")
    void rejectsNullRandom() {
        assertThatThrownBy(() -> new RandomLottoNumberGenerator(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("random must not be null");
    }
}

