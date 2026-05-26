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
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("로또 조합 속성 테스트")
class LottoCombinationPropertyTest {

    private static final int GENERATED_COMBINATION_TRIES = 200;
    private static final int RECOMMENDATION_TRIES = 100;
    private static final int MIN_RECOMMENDATION_COUNT = 1;
    private static final int MAX_RECOMMENDATION_COUNT = 10;
    private static final int MAX_RECOMMENDATION_ATTEMPTS = 10_000;

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

    private static final LottoRecommender RECOMMENDER =
            new LottoRecommender(RULES, GENERATOR, MAX_RECOMMENDATION_ATTEMPTS);

    @RepeatedTest(GENERATED_COMBINATION_TRIES)
    @DisplayName("생성된 모든 조합은 유효해야 한다")
    void everyGeneratedCombinationIsValid() {
        LottoCombination combo = GENERATOR.generate();

        assertValidCombination(combo);
    }

    @ParameterizedTest(name = "{0}개 추천")
    @MethodSource("recommendationCounts")
    @DisplayName("추천된 모든 조합은 모든 제외 규칙을 통과해야 한다")
    void everyRecommendedCombinationPassesAllRules(int count) {
        List<LottoCombination> batch = RECOMMENDER.recommend(count);

        assertThat(batch).hasSize(count);
        assertThat(batch).allSatisfy(LottoCombinationPropertyTest::assertValidCombination);
        assertThat(batch).allSatisfy(LottoCombinationPropertyTest::assertPassesAllRules);
    }

    @RepeatedTest(RECOMMENDATION_TRIES)
    @DisplayName("추천된 모든 조합은 모든 제외 규칙을 반복해서 통과해야 한다")
    void everyRecommendedCombinationRepeatedlyPassesAllRules() {
        List<LottoCombination> batch = RECOMMENDER.recommend(MAX_RECOMMENDATION_COUNT);

        assertThat(batch).hasSize(MAX_RECOMMENDATION_COUNT);
        assertThat(batch).allSatisfy(LottoCombinationPropertyTest::assertValidCombination);
        assertThat(batch).allSatisfy(LottoCombinationPropertyTest::assertPassesAllRules);
    }

    @ParameterizedTest(name = "{0}개 추천")
    @MethodSource("recommendationCounts")
    @DisplayName("추천 배치는 중복된 조합을 포함하지 않아야 한다")
    void batchHasNoDuplicateCombinations(int count) {
        List<LottoCombination> batch = RECOMMENDER.recommend(count);

        assertThat(batch).hasSize(count);
        assertThat(batch).doesNotHaveDuplicates();
    }

    static Stream<Integer> recommendationCounts() {
        return IntStream.rangeClosed(MIN_RECOMMENDATION_COUNT, MAX_RECOMMENDATION_COUNT).boxed();
    }

    private static void assertValidCombination(LottoCombination combination) {
        assertThat(combination.numbers()).hasSize(LottoCombination.SIZE);
        assertThat(combination.numbers())
                .allMatch(number -> number >= LottoCombination.MIN_NUMBER
                        && number <= LottoCombination.MAX_NUMBER);
        assertThat(combination.numbers()).doesNotHaveDuplicates();
        assertThat(combination.numbers()).isSorted();
    }

    private static void assertPassesAllRules(LottoCombination combination) {
        for (ExclusionRule rule : RULES) {
            assertThat(rule.shouldExclude(combination))
                    .as("Rule %s must not exclude recommended combination %s",
                            rule.name(),
                            combination.numbers())
                    .isFalse();
        }
    }
}
