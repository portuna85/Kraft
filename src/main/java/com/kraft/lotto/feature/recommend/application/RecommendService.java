package com.kraft.lotto.feature.recommend.application;

import com.kraft.lotto.feature.recommend.domain.ExclusionRule;
import com.kraft.lotto.feature.recommend.web.dto.CombinationDto;
import com.kraft.lotto.feature.recommend.web.dto.RecommendResponse;
import com.kraft.lotto.feature.recommend.web.dto.RuleDto;
import com.kraft.lotto.support.BusinessException;
import com.kraft.lotto.support.ErrorCode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
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
        long started = System.nanoTime();
        int safeCount = normalizeCount(count);
        metricsRecorder.recordRequestedCount(safeCount);
        try {
            var combinations = recommender.recommend(safeCount).stream()
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

    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "ruleDtos is built once as an unmodifiable stream result and RuleDto is immutable")
    public List<RuleDto> rules() {
        return ruleDtos;
    }

    private static int normalizeCount(int count) {
        return (int) Math.clamp(count, MIN_COUNT, MAX_COUNT);
    }

}
