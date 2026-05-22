package com.kraft.lotto.feature.recommend.application;

import com.kraft.lotto.feature.recommend.domain.ExclusionRule;
import com.kraft.lotto.feature.recommend.web.dto.CombinationDto;
import com.kraft.lotto.feature.recommend.web.dto.RecommendResponse;
import com.kraft.lotto.feature.recommend.web.dto.RuleDto;
import com.kraft.lotto.support.BusinessException;
import com.kraft.lotto.support.ErrorCode;
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
            throw new BusinessException(ErrorCode.LOTTO_GENERATION_TIMEOUT, ex.getMessage(), ex);
        } finally {
            recordLatency(started);
        }
    }

    public List<RuleDto> rules() {
        return rules.stream()
                .map(r -> new RuleDto(r.name(), r.reason()))
                .toList();
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
}
