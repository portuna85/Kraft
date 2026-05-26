package com.kraft.lotto.feature.recommend.application;

import com.kraft.lotto.feature.recommend.domain.ExclusionRule;
import com.kraft.lotto.infra.config.KraftRecommendProperties;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RecommendConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RecommendConfiguration.class);

    private static final int LOTTO_NUMBER_COUNT = 6;
    private static final int DECADE_BUCKET_COUNT = 5;

    @Bean
    LottoNumberGenerator lottoNumberGenerator(KraftRecommendProperties properties) {
        KraftRecommendProperties.Rules rules = properties.rules();
        int maxPerBucket = Math.max(1, rules.decadeThreshold() - 1);
        if (maxPerBucket * DECADE_BUCKET_COUNT < LOTTO_NUMBER_COUNT) {
            throw new IllegalStateException(
                    "kraft.recommend.rules.decade-threshold=" + rules.decadeThreshold()
                    + " is infeasible: max " + maxPerBucket + " per bucket × " + DECADE_BUCKET_COUNT
                    + " buckets = " + (maxPerBucket * DECADE_BUCKET_COUNT)
                    + " < " + LOTTO_NUMBER_COUNT + " numbers required. Increase to at least 3.");
        }
        return new ConstraintAwareLottoNumberGenerator(
                rules.birthdayThreshold(),
                rules.longRunThreshold(),
                rules.decadeThreshold(),
                properties.initialPickMaxAttempts(),
                properties.fixupMaxAttempts()
        );
    }

    @Bean
    LottoRecommender lottoRecommender(@Qualifier("exclusionRules") List<ExclusionRule> rules,
                                      LottoNumberGenerator numberGenerator,
                                      KraftRecommendProperties properties,
                                      ObjectProvider<MeterRegistry> meterRegistryProvider) {
        return new LottoRecommender(
                rules,
                numberGenerator,
                properties.maxAttempts(),
                meterRegistryProvider.getIfAvailable()
        );
    }
}
