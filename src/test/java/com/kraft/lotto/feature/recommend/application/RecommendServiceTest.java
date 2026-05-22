package com.kraft.lotto.feature.recommend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.kraft.lotto.feature.recommend.domain.BirthdayBiasRule;
import com.kraft.lotto.feature.recommend.domain.ExclusionRule;
import com.kraft.lotto.feature.recommend.domain.ArithmeticSequenceRule;
import com.kraft.lotto.feature.recommend.domain.LongRunRule;
import com.kraft.lotto.feature.recommend.domain.SingleDecadeRule;
import com.kraft.lotto.feature.winningnumber.domain.LottoCombination;
import com.kraft.lotto.support.BusinessException;
import com.kraft.lotto.support.ErrorCode;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("추천 서비스 테스트")
class RecommendServiceTest {

    private static final long FIXED_SEED = 1L;

    private RecommendService service(List<ExclusionRule> rules) {
        return new RecommendService(rules, new LottoRecommender(rules, new Random(FIXED_SEED), 100_000), (MeterRegistry) null);
    }

    private static ExclusionRule excludeAll() {
        return new ExclusionRule() {
            @Override public boolean shouldExclude(LottoCombination combination) { return true; }
            @Override public String reason() { return "always-exclude"; }
        };
    }

    @Test
    @DisplayName("추천 개수가 0이면 1로 보정하여 추천한다")
    void clampsZeroCountToOne() {
        var service = service(List.of());

        var response = service.recommend(0);

        assertThat(response.combinations()).hasSize(1);
    }

    @Test
    @DisplayName("추천 개수가 11이면 10으로 보정하여 추천한다")
    void clampsElevenCountToTen() {
        var service = service(List.of());

        var response = service.recommend(11);

        assertThat(response.combinations()).hasSize(10);
    }

    @Test
    @DisplayName("추천 개수가 1이면 한 개의 조합을 반환한다")
    void returnsOneCombinationWhenCountIsOne() {
        var service = service(List.of(new BirthdayBiasRule()));

        var response = service.recommend(1);

        assertThat(response.combinations()).hasSize(1);
    }

    @Test
    @DisplayName("추천 개수가 10이면 열 개의 조합을 반환한다")
    void returnsTenCombinationsWhenCountIsTen() {
        var service = service(List.of(new BirthdayBiasRule()));

        var response = service.recommend(10);

        assertThat(response.combinations()).hasSize(10);
    }

    @Test
    @DisplayName("요청된 개수만큼의 조합을 반환한다")
    void returnsRequestedNumberOfCombinations() {
        var service = service(List.of(new BirthdayBiasRule()));

        var response = service.recommend(5);

        assertThat(response.combinations()).hasSize(5);
    }

    @Test
    @DisplayName("모든 조합이 제외되면 생성 타임아웃 예외가 발생한다")
    void throwsGenerationTimeoutWhenAllExcluded() {
        List<ExclusionRule> rules = List.of(excludeAll());
        var service = new RecommendService(rules, new LottoRecommender(rules, new Random(FIXED_SEED), 50), (MeterRegistry) null);

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> service.recommend(1))
                .extracting(BusinessException::getErrorCode)
                .isEqualTo(ErrorCode.LOTTO_GENERATION_TIMEOUT);
    }

    @Test
    @DisplayName("등록된 규칙 이름과 사유를 반환한다")
    void rulesReturnsRegisteredRuleNamesAndReasons() {
        var service = service(List.of(new BirthdayBiasRule()));

        var rules = service.rules();

        assertThat(rules).hasSize(1);
        assertThat(rules.get(0).name()).isEqualTo("BirthdayBiasRule");
        assertThat(rules.get(0).reason()).isNotBlank();
    }

    @Test
    @DisplayName("다중 규칙 조합에서도 반환된 번호는 어떤 규칙에도 제외되지 않는다")
    void recommendedCombinationsPassAllIntegratedRules() {
        List<ExclusionRule> rules = List.of(
                new BirthdayBiasRule(),
                new ArithmeticSequenceRule(),
                new LongRunRule(5),
                new SingleDecadeRule(5)
        );
        var service = service(rules);

        var response = service.recommend(10);

        assertThat(response.combinations()).hasSize(10);
        response.combinations().forEach(combinationDto -> {
            LottoCombination combination = new LottoCombination(combinationDto.numbers());
            assertThat(rules.stream().noneMatch(rule -> rule.shouldExclude(combination))).isTrue();
        });
    }

    @Test
    @DisplayName("요청 개수 메트릭을 기록한다")
    void recordsRequestedCountMetric() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        List<ExclusionRule> rules = List.of(new BirthdayBiasRule());
        var service = new RecommendService(
                rules,
                new LottoRecommender(rules, new RandomLottoNumberGenerator(new Random(FIXED_SEED)), 100_000, registry),
                registry
        );

        service.recommend(3);

        assertThat(registry.find("kraft.recommend.request.count").summary()).isNotNull();
    }

    @Test
    @DisplayName("추천 생성 실패 reason 메트릭을 기록한다")
    void recordsFailureReasonMetricOnTimeout() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        List<ExclusionRule> rules = List.of(excludeAll());
        var service = new RecommendService(
                rules,
                new LottoRecommender(rules, new RandomLottoNumberGenerator(new Random(FIXED_SEED)), 5, registry),
                registry
        );

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> service.recommend(1))
                .extracting(BusinessException::getErrorCode)
                .isEqualTo(ErrorCode.LOTTO_GENERATION_TIMEOUT);

        assertThat(registry.get("kraft.recommend.generation.failure")
                .tag("reason", "attempt_exhausted")
                .counter()
                .count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("initial pick timeout 실패 reason 메트릭을 기록한다")
    void recordsInitialPickTimeoutReasonMetric() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        LottoRecommender failingRecommender = new LottoRecommender(List.of(), new Random(FIXED_SEED), 1) {
            @Override
            public List<LottoCombination> recommend(int count) {
                throw new RecommendGenerationTimeoutException(
                        "initial pick exceeded max attempts",
                        RecommendGenerationTimeoutException.FailureReason.INITIAL_PICK_TIMEOUT
                );
            }
        };
        var service = new RecommendService(List.of(), failingRecommender, registry);

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> service.recommend(1))
                .extracting(BusinessException::getErrorCode)
                .isEqualTo(ErrorCode.LOTTO_GENERATION_TIMEOUT);

        assertThat(registry.get("kraft.recommend.generation.failure")
                .tag("reason", "initial_pick_timeout")
                .counter()
                .count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("fixup timeout 실패 reason 메트릭을 기록한다")
    void recordsFixupTimeoutReasonMetric() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        LottoRecommender failingRecommender = new LottoRecommender(List.of(), new Random(FIXED_SEED), 1) {
            @Override
            public List<LottoCombination> recommend(int count) {
                throw new RecommendGenerationTimeoutException(
                        "fixup exceeded max attempts",
                        RecommendGenerationTimeoutException.FailureReason.FIXUP_TIMEOUT
                );
            }
        };
        var service = new RecommendService(List.of(), failingRecommender, registry);

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> service.recommend(1))
                .extracting(BusinessException::getErrorCode)
                .isEqualTo(ErrorCode.LOTTO_GENERATION_TIMEOUT);

        assertThat(registry.get("kraft.recommend.generation.failure")
                .tag("reason", "fixup_timeout")
                .counter()
                .count()).isEqualTo(1.0);
    }
}
