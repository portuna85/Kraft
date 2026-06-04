package com.kraft.lotto.feature.recommend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kraft.lotto.feature.recommend.domain.ArithmeticSequenceRule;
import com.kraft.lotto.feature.recommend.domain.BirthdayBiasRule;
import com.kraft.lotto.feature.recommend.domain.ExclusionRule;
import com.kraft.lotto.feature.winningnumber.domain.LottoCombination;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("로또 추천기")
class LottoRecommenderTest {

    private static final int LARGE_BUDGET = 100_000;

    private static LottoRecommender recommender(List<ExclusionRule> rules, long seed, int maxAttempts) {
        return new LottoRecommender(rules, new Random(seed), maxAttempts);
    }

    private static ExclusionRule excludeAll() {
        return new ExclusionRule() {
            @Override
            public boolean shouldExclude(LottoCombination combination) {
                return true;
            }

            @Override
            public String reason() {
                return "always";
            }
        };
    }

    @Test
    @DisplayName("요청된 개수만큼 추천 번호를 반환한다")
    void recommendsRequestedCount() {
        List<LottoCombination> result = recommender(List.of(), 42L, LARGE_BUDGET).recommend(5);

        assertThat(result).hasSize(5);
    }

    @Test
    @DisplayName("생성된 각 조합은 6개의 숫자를 가진다")
    void generatedCombinationsHaveSixNumbers() {
        List<LottoCombination> result = recommender(List.of(), 42L, LARGE_BUDGET).recommend(5);

        assertThat(result).allSatisfy(combination -> assertThat(combination.numbers()).hasSize(6));
    }

    @Test
    @DisplayName("생성된 각 조합은 정렬되어 있다")
    void generatedCombinationsAreSorted() {
        List<LottoCombination> result = recommender(List.of(), 42L, LARGE_BUDGET).recommend(5);

        assertThat(result).allSatisfy(combination -> assertThat(combination.numbers()).isSorted());
    }

    @Test
    @DisplayName("생성된 각 조합에 중복된 숫자가 없다")
    void generatedCombinationsHaveNoDuplicateNumbers() {
        List<LottoCombination> result = recommender(List.of(), 42L, LARGE_BUDGET).recommend(5);

        assertThat(result).allSatisfy(combination -> assertThat(combination.numbers()).doesNotHaveDuplicates());
    }

    @Test
    @DisplayName("추천 결과는 서로 중복되지 않는다")
    void recommendationsAreUnique() {
        List<LottoCombination> result = recommender(List.of(), 7L, LARGE_BUDGET).recommend(10);

        assertThat(result).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("중복된 후보는 거절되고 생성을 계속한다")
    void duplicateCandidatesAreRejected() {
        LottoCombination first = LottoCombination.of(1, 2, 3, 4, 5, 6);
        LottoCombination second = LottoCombination.of(7, 8, 9, 10, 11, 12);
        LottoRecommender recommender = new LottoRecommender(
                List.of(),
                generator(first, first, second),
                3
        );

        List<LottoCombination> result = recommender.recommend(2);

        assertThat(result).containsExactly(first, second);
    }

    @Test
    @DisplayName("모든 후보가 제외되면 예외가 발생한다")
    void throwsTimeoutWhenAllCombinationsExcluded() {
        LottoRecommender recommender = recommender(List.of(excludeAll()), 0L, 100);

        assertThatThrownBy(() -> recommender.recommend(1))
                .isInstanceOf(RecommendGenerationTimeoutException.class);
    }

    @Test
    @DisplayName("등록된 규칙에 부합하는 후보는 제외된다")
    void rulesAreAppliedAndExcludedCombinationsNotIncluded() {
        List<LottoCombination> result = recommender(List.of(new BirthdayBiasRule()), 123L, LARGE_BUDGET)
                .recommend(10);

        assertThat(result).allSatisfy(combination ->
                assertThat(combination.numbers().stream().anyMatch(number -> number > 31)).isTrue()
        );
    }

    @Test
    @DisplayName("여러 규칙이 함께 적용되어 후보를 검증한다")
    void multipleRulesAreAppliedTogether() {
        LottoCombination birthdayExcluded = LottoCombination.of(1, 2, 3, 4, 5, 6);
        LottoCombination arithmeticExcluded = LottoCombination.of(1, 8, 15, 22, 29, 36);
        LottoCombination allowed = LottoCombination.of(2, 9, 17, 24, 35, 41);
        LottoRecommender recommender = new LottoRecommender(
                List.of(new BirthdayBiasRule(), new ArithmeticSequenceRule()),
                generator(birthdayExcluded, arithmeticExcluded, allowed),
                3
        );

        List<LottoCombination> result = recommender.recommend(1);

        assertThat(result).containsExactly(allowed);
    }

    @Test
    @DisplayName("명시 규칙 추천은 생성자 규칙 대신 전달된 규칙만 적용한다")
    void recommendWithRulesUsesOnlyProvidedRules() {
        LottoCombination allowedByProvidedRules = LottoCombination.of(2, 9, 17, 24, 35, 41);
        LottoRecommender recommender = new LottoRecommender(
                List.of(excludeAll()),
                generator(allowedByProvidedRules),
                1
        );

        List<LottoCombination> result = recommender.recommendWithRules(1, List.of());

        assertThat(result).containsExactly(allowedByProvidedRules);
    }

    @Test
    @DisplayName("제외 지표를 기록한다")
    void recordsRejectionMetrics() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        LottoRecommender recommender = new LottoRecommender(
                List.of(),
                new RandomLottoNumberGenerator(new Random(42L)),
                LARGE_BUDGET,
                registry
        );

        recommender.recommend(5);

        assertThat(registry.find("kraft.recommend.rejection.rate").summary()).isNotNull();
        assertThat(registry.find("kraft.recommend.rejection.count").counter()).isNotNull();
        assertThat(registry.find("kraft.recommend.attempt.count").counter()).isNotNull();
    }

    @Test
    @DisplayName("규칙 거절 메트릭을 규칙명 태그로 기록한다")
    void recordsRuleRejectionMetricByRuleName() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        LottoCombination excluded = LottoCombination.of(1, 2, 3, 4, 5, 6);
        LottoCombination allowed = LottoCombination.of(2, 9, 17, 24, 35, 41);
        ExclusionRule alwaysExcludeSpecific = new ExclusionRule() {
            @Override
            public boolean shouldExclude(LottoCombination combination) {
                return excluded.equals(combination);
            }

            @Override
            public String name() {
                return "specific-rule";
            }

            @Override
            public String reason() {
                return "test";
            }
        };
        LottoRecommender recommender = new LottoRecommender(
                List.of(alwaysExcludeSpecific),
                generator(excluded, allowed),
                10,
                registry
        );

        recommender.recommend(1);

        assertThat(registry.find("kraft.recommend.rejection.by.type").tag("type", "rule").counter())
                .isNotNull();
        assertThat(registry.find("kraft.recommend.rejection.by.rule").tag("rule", "specific-rule").counter())
                .isNotNull();
    }

    private static LottoNumberGenerator generator(LottoCombination... combinations) {
        AtomicInteger index = new AtomicInteger();
        return () -> combinations[Math.min(index.getAndIncrement(), combinations.length - 1)];
    }
}
