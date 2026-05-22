package com.kraft.lotto.feature.recommend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kraft.lotto.feature.recommend.domain.ExclusionRule;
import com.kraft.lotto.infra.config.KraftRecommendProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

@DisplayName("추천 설정")
class RecommendConfigurationTest {

    private final RecommendConfiguration config = new RecommendConfiguration();

    private static final KraftRecommendProperties PROPERTIES = new KraftRecommendProperties(
            1234,
            4321,
            321,
            new KraftRecommendProperties.Rules(31, 5, 5)
    );

    @Test
    @DisplayName("설정값으로부터 제약 조건 기반 번호 생성기를 생성한다")
    void createsNumberGenerator() {
        LottoNumberGenerator generator = config.lottoNumberGenerator(PROPERTIES);

        assertThat(generator).isInstanceOf(ConstraintAwareLottoNumberGenerator.class);
    }

    @Test
    @DisplayName("제공된 규칙과 생성기를 사용하여 추천기를 구성한다")
    void createsRecommender() {
        ExclusionRule noOpRule = new ExclusionRule() {
            @Override
            public boolean shouldExclude(com.kraft.lotto.feature.winningnumber.domain.LottoCombination combination) {
                return false;
            }

            @Override
            public String reason() {
                return "no-op";
            }
        };
        LottoNumberGenerator generator = config.lottoNumberGenerator(PROPERTIES);
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        @SuppressWarnings("unchecked")
        ObjectProvider<MeterRegistry> meterRegistryProvider = mock(ObjectProvider.class);
        when(meterRegistryProvider.getIfAvailable()).thenReturn(meterRegistry);

        LottoRecommender recommender = config.lottoRecommender(
                List.of(noOpRule),
                generator,
                PROPERTIES,
                meterRegistryProvider
        );

        assertThat(recommender.recommend(1)).hasSize(1);
    }
}
