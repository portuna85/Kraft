package com.kraft.lotto.feature.recommend.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.kraft.lotto.feature.recommend.domain.ArithmeticSequenceRule;
import com.kraft.lotto.feature.recommend.domain.BirthdayBiasRule;
import com.kraft.lotto.feature.recommend.domain.ExclusionRule;
import com.kraft.lotto.feature.recommend.domain.LongRunRule;
import com.kraft.lotto.feature.recommend.domain.PastWinningCache;
import com.kraft.lotto.feature.recommend.domain.PastWinningRule;
import com.kraft.lotto.feature.recommend.domain.SingleDecadeRule;
import com.kraft.lotto.feature.winningnumber.domain.LottoCombination;
import java.util.List;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.DisplayName;

@DisplayName("로또 조합 속성 테스트")
class LottoCombinationPropertyTest {

    private static final List<ExclusionRule> RULES = List.of(
            new BirthdayBiasRule(),
            new ArithmeticSequenceRule(),
            new LongRunRule(),
            new SingleDecadeRule(),
            new PastWinningRule(new PastWinningCache())
    );

    private static final ConstraintAwareLottoNumberGenerator GENERATOR =
            new ConstraintAwareLottoNumberGenerator(
                    BirthdayBiasRule.DEFAULT_BIRTHDAY_THRESHOLD,
                    LongRunRule.DEFAULT_LONG_RUN_THRESHOLD,
                    SingleDecadeRule.DEFAULT_DECADE_THRESHOLD
            );

    private static final LottoRecommender RECOMMENDER = new LottoRecommender(RULES, GENERATOR, 500);

    @Property(tries = 200)
    @DisplayName("생성된 모든 조합은 유효해야 한다")
    void everyGeneratedCombinationIsValid() {
        LottoCombination combo = GENERATOR.generate();

        assertThat(combo.numbers()).hasSize(LottoCombination.SIZE);
        assertThat(combo.numbers()).allMatch(n -> n >= LottoCombination.MIN_NUMBER && n <= LottoCombination.MAX_NUMBER);
        assertThat(combo.numbers()).doesNotHaveDuplicates();
        assertThat(combo.numbers()).isSorted();
    }

    @Property(tries = 100)
    @DisplayName("추천된 모든 조합은 모든 제외 규칙을 통과해야 한다")
    void everyRecommendedCombinationPassesAllRules() {
        List<LottoCombination> batch = RECOMMENDER.recommend(5);

        for (LottoCombination combo : batch) {
            assertThat(combo.numbers()).hasSize(LottoCombination.SIZE);
            assertThat(combo.numbers()).allMatch(n -> n >= LottoCombination.MIN_NUMBER && n <= LottoCombination.MAX_NUMBER);
            assertThat(combo.numbers()).doesNotHaveDuplicates();
            assertThat(combo.numbers()).isSorted();
            for (ExclusionRule rule : RULES) {
                assertThat(rule.shouldExclude(combo))
                        .as("Rule %s must not exclude recommended combo %s", rule.name(), combo.numbers())
                        .isFalse();
            }
        }
    }

    @Property(tries = 100)
    @DisplayName("추천 배치는 중복된 조합을 포함하지 않아야 한다")
    void batchHasNoDuplicateCombinations(@ForAll @IntRange(min = 1, max = 10) int count) {
        List<LottoCombination> batch = RECOMMENDER.recommend(count);

        assertThat(batch).hasSize(count);
        assertThat(batch).doesNotHaveDuplicates();
    }
}
