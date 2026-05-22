package com.kraft.lotto.feature.recommend.application;

import com.kraft.lotto.feature.recommend.domain.ExclusionRule;
import com.kraft.lotto.infra.config.KraftRecommendProperties;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RecommendConfiguration {

    @Bean
    LottoNumberGenerator lottoNumberGenerator(KraftRecommendProperties properties) {
        KraftRecommendProperties.Rules rules = properties.rules();
        return new ConstraintAwareLottoNumberGenerator(
                rules.birthdayThreshold(),
                rules.longRunThreshold(),
                rules.decadeThreshold(),
                properties.initialPickMaxAttempts(),
                properties.fixupMaxAttempts()
        );
    }

    @Bean
    LottoRecommender lottoRecommender(List<ExclusionRule> rules,
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
