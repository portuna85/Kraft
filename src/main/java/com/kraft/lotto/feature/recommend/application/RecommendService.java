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
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RecommendService {

    static final int MIN_COUNT = 1;
    static final int MAX_COUNT = 10;

    private final List<ExclusionRule> rules;
    private final List<RuleDto> ruleDtos;
    private final LottoRecommender recommender;
    private final MeterRegistry meterRegistry;

    @Autowired
    public RecommendService(List<ExclusionRule> rules,
                            LottoRecommender recommender,
                            ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this(rules, recommender, meterRegistryProvider.getIfAvailable());
    }

    RecommendService(List<ExclusionRule> rules, LottoRecommender recommender, MeterRegistry meterRegistry) {
        this.rules = List.copyOf(rules);
        this.ruleDtos = this.rules.stream()
                .map(r -> new RuleDto(r.name(), r.reason()))
                .toList();
        this.recommender = recommender;
        this.meterRegistry = meterRegistry;
    }

    public RecommendResponse recommend(int count) {
        long started = System.nanoTime();
        int safeCount = normalizeCount(count);
        recordRequestedCount(safeCount);
        try {
            var combinations = recommender.recommend(safeCount).stream()
                    .map(c -> new CombinationDto(c.numbers()))
                    .toList();
            return new RecommendResponse(combinations);
        } catch (RecommendGenerationTimeoutException ex) {
            recordFailure(ex);
            throw new BusinessException(ErrorCode.LOTTO_GENERATION_TIMEOUT, ex.getMessage(), ex);
        } finally {
            recordLatency(started);
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

    private void recordLatency(long startedAtNanos) {
        if (meterRegistry == null) {
            return;
        }
        meterRegistry.timer("kraft.recommend.generation.latency")
                .record(System.nanoTime() - startedAtNanos, TimeUnit.NANOSECONDS);
    }

    private void recordRequestedCount(int count) {
        if (meterRegistry == null) {
            return;
        }
        meterRegistry.summary("kraft.recommend.request.count").record(count);
    }

    private void recordFailure(RecommendGenerationTimeoutException ex) {
        if (meterRegistry == null) {
            return;
        }
        String reason = switch (ex.getReason()) {
            case ATTEMPT_EXHAUSTED -> "attempt_exhausted";
            case INITIAL_PICK_TIMEOUT -> "initial_pick_timeout";
            case FIXUP_TIMEOUT -> "fixup_timeout";
            case OTHER -> "other";
        };
        meterRegistry.counter("kraft.recommend.generation.failure", "reason", reason).increment();
    }
}
