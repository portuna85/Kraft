package com.kraft.lotto.feature.recommend.application;

import com.kraft.lotto.feature.recommend.domain.ExclusionRule;
import com.kraft.lotto.feature.recommend.web.dto.CombinationDto;
import com.kraft.lotto.feature.recommend.web.dto.RecommendResponse;
import com.kraft.lotto.feature.recommend.web.dto.RuleDto;
import com.kraft.lotto.feature.winningnumber.domain.LottoCombination;
import com.kraft.lotto.support.BusinessException;
import com.kraft.lotto.support.ErrorCode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RecommendService {

    static final int MIN_COUNT = 1;
    static final int MAX_COUNT = 10;

    private final List<ExclusionRule> rules;
    private final List<RuleDto> ruleDtos;
    private final LottoRecommender recommender;
    private final RecommendMetricsRecorder metricsRecorder;

    @Autowired
    public RecommendService(List<ExclusionRule> rules,
                            LottoRecommender recommender,
                            RecommendMetricsRecorder metricsRecorder) {
        this.rules = List.copyOf(rules);
        this.ruleDtos = this.rules.stream()
                .map(r -> new RuleDto(r.name(), r.reason()))
                .toList();
        this.recommender = recommender;
        this.metricsRecorder = metricsRecorder;
    }

    RecommendService(List<ExclusionRule> rules, LottoRecommender recommender, MeterRegistry meterRegistry) {
        this(rules, recommender, new RecommendMetricsRecorder(meterRegistry));
    }

    public RecommendResponse recommend(int count) {
        return recommend(count, RecommendFilter.NONE);
    }

    public RecommendResponse recommend(int count, RecommendFilter filter) {
        long started = System.nanoTime();
        int validatedCount = validateCount(count);
        metricsRecorder.recordRequestedCount(validatedCount);
        try {
            List<ExclusionRule> filterRules = buildFilterRules(filter);
            var combinations = recommender.recommend(validatedCount, filterRules).stream()
                    .map(c -> new CombinationDto(c.numbers()))
                    .toList();
            return new RecommendResponse(combinations);
        } catch (RecommendGenerationTimeoutException ex) {
            metricsRecorder.recordFailure(ex);
            throw new BusinessException(ErrorCode.LOTTO_GENERATION_TIMEOUT, ex.getMessage(), ex);
        } finally {
            metricsRecorder.recordLatency(started);
        }
    }

    private static List<ExclusionRule> buildFilterRules(RecommendFilter filter) {
        if (filter == null || filter.isNone()) {
            return List.of();
        }
        List<ExclusionRule> filterRules = new ArrayList<>(2);
        if (filter.hasOddCount()) {
            filterRules.add(new OddCountFilterRule(filter.oddCount()));
        }
        if (filter.hasSumRange()) {
            filterRules.add(new SumRangeFilterRule(filter.sumMin(), filter.sumMax()));
        }
        return List.copyOf(filterRules);
    }

    private static final class OddCountFilterRule implements ExclusionRule {
        private final int target;

        OddCountFilterRule(int target) {
            this.target = target;
        }

        @Override public boolean shouldExclude(LottoCombination c) { return c.oddCount() != target; }
        @Override public String reason() { return "홀수 개수 " + target + "개 조건 불일치"; }
        @Override public String name() { return "OddCountFilter"; }
    }

    private static final class SumRangeFilterRule implements ExclusionRule {
        private final Integer min;
        private final Integer max;

        SumRangeFilterRule(Integer min, Integer max) {
            this.min = min;
            this.max = max;
        }

        @Override public boolean shouldExclude(LottoCombination c) {
            int s = c.sum();
            return (min != null && s < min) || (max != null && s > max);
        }
        @Override public String reason() { return "합산 범위 조건 불일치"; }
        @Override public String name() { return "SumRangeFilter"; }
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "ruleDtos is built once as an unmodifiable stream result and RuleDto is immutable")
    public List<RuleDto> rules() {
        return ruleDtos;
    }

    private static int validateCount(int count) {
        if (count < MIN_COUNT || count > MAX_COUNT) {
            throw new BusinessException(
                    ErrorCode.LOTTO_INVALID_COUNT,
                    "count must be between " + MIN_COUNT + " and " + MAX_COUNT
            );
        }
        return count;
    }

}
