package com.kraft.lotto.feature.recommend.application;

import com.kraft.lotto.feature.recommend.domain.ExclusionRule;
import com.kraft.lotto.feature.winningnumber.domain.LottoCombination;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

@SuppressFBWarnings(
        value = {"EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"},
        justification = "Dependencies are intentionally injected and constructor precondition checks fail fast")
public class LottoRecommender {

    private final List<ExclusionRule> rules;
    private final LottoNumberGenerator numberGenerator;
    private final int maxAttempts;
    private final MeterRegistry meterRegistry;

    public LottoRecommender(List<ExclusionRule> rules, LottoNumberGenerator numberGenerator, int maxAttempts) {
        this(rules, numberGenerator, maxAttempts, new SimpleMeterRegistry());
    }

    public LottoRecommender(List<ExclusionRule> rules,
                            LottoNumberGenerator numberGenerator,
                            int maxAttempts,
                            MeterRegistry meterRegistry) {
        if (rules == null) {
            throw new IllegalArgumentException("rules must not be null");
        }
        if (numberGenerator == null) {
            throw new IllegalArgumentException("numberGenerator must not be null");
        }
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("maxAttempts must be positive: " + maxAttempts);
        }
        this.rules = List.copyOf(rules);
        this.numberGenerator = numberGenerator;
        this.maxAttempts = maxAttempts;
        this.meterRegistry = meterRegistry;
    }

    public LottoRecommender(List<ExclusionRule> rules, Random random, int maxAttempts) {
        this(rules, new RandomLottoNumberGenerator(random), maxAttempts, new SimpleMeterRegistry());
    }

    public List<LottoCombination> recommend(int count) {
        return recommend(count, List.of());
    }

    /**
     * 제공된 규칙 목록만 적용한다 (this.rules 무시).
     * 사용자가 일부 규칙을 비활성화한 경우에 사용된다.
     */
    public List<LottoCombination> recommendWithRules(int count, List<ExclusionRule> effectiveRules) {
        return recommendInternal(count, effectiveRules);
    }

    public List<LottoCombination> recommend(int count, List<ExclusionRule> additionalRules) {
        List<ExclusionRule> safeAdditionalRules = additionalRules == null ? List.of() : additionalRules;
        List<ExclusionRule> effectiveRules = new ArrayList<>(rules.size() + safeAdditionalRules.size());
        effectiveRules.addAll(rules);
        effectiveRules.addAll(safeAdditionalRules);
        return recommendInternal(count, effectiveRules);
    }

    private List<LottoCombination> recommendInternal(int count, List<ExclusionRule> effectiveRules) {
        if (count <= 0) {
            throw new IllegalArgumentException("count must be positive: " + count);
        }
        if (effectiveRules == null) {
            throw new IllegalArgumentException("effectiveRules must not be null");
        }
        List<ExclusionRule> rulesToApply = List.copyOf(effectiveRules);
        List<LottoCombination> result = new ArrayList<>(count);
        Set<LottoCombination> emitted = new LinkedHashSet<>();
        int attempts = 0;
        int rejected = 0;
        while (result.size() < count) {
            if (attempts >= maxAttempts) {
                recordRejectionRate(attempts, rejected);
                recordTimeout();
                throw new RecommendGenerationTimeoutException(
                        "recommend generation attempts exceeded (max=" + maxAttempts + ")",
                        RecommendGenerationTimeoutException.FailureReason.ATTEMPT_EXHAUSTED
                );
            }
            attempts++;
            LottoCombination candidate = numberGenerator.generate();
            if (emitted.contains(candidate)) {
                rejected++;
                recordDuplicateRejection();
                continue;
            }
            String excludedRule = findExcludedRule(candidate, rulesToApply);
            if (excludedRule != null) {
                rejected++;
                recordRuleRejection(excludedRule);
                continue;
            }
            emitted.add(candidate);
            result.add(candidate);
        }
        recordRejectionRate(attempts, rejected);
        return List.copyOf(result);
    }

    private String findExcludedRule(LottoCombination candidate, List<ExclusionRule> rulesToApply) {
        for (ExclusionRule rule : rulesToApply) {
            if (rule.shouldExclude(candidate)) {
                return rule.name();
            }
        }
        return null;
    }


    private void recordRejectionRate(int attempts, int rejected) {
        if (attempts <= 0) {
            return;
        }
        meterRegistry.summary("kraft.recommend.rejection.rate")
                .record((double) rejected / attempts);
        meterRegistry.counter("kraft.recommend.rejection.count").increment(rejected);
        meterRegistry.counter("kraft.recommend.attempt.count").increment(attempts);
    }

    private void recordDuplicateRejection() {
        meterRegistry.counter("kraft.recommend.rejection.by.type", "type", "duplicate").increment();
        meterRegistry.counter("kraft.recommend.generator.reject.total", "reason", "duplicate").increment();
    }

    private void recordRuleRejection(String ruleName) {
        meterRegistry.counter("kraft.recommend.rejection.by.type", "type", "rule").increment();
        meterRegistry.counter("kraft.recommend.rejection.by.rule", "rule", ruleName).increment();
        meterRegistry.counter("kraft.recommend.generator.reject.total", "reason", "rule_" + ruleName).increment();
    }

    private void recordTimeout() {
        meterRegistry.counter("kraft.recommend.timeout.count").increment();
        meterRegistry.counter("kraft.recommend.generator.timeout.total",
                "phase", "attempt_exhausted").increment();
    }
}
