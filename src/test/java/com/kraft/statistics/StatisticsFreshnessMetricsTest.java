package com.kraft.statistics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("통계 최신성 지표 단위 테스트")
class StatisticsFreshnessMetricsTest {

    @Mock
    private FrequencySummaryRepository frequencySummaryRepository;

    @Test
    @DisplayName("summary의 최대 lastRound가 게이지 값이 된다")
    void gauge_reflectsMaxLastRound() {
        given(frequencySummaryRepository.findMaxLastRound()).willReturn(1230);

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        new StatisticsFreshnessMetrics(registry, frequencySummaryRepository);

        double value = registry.get("kraft_lotto_statistics_projected_round").gauge().value();
        assertThat(value).isEqualTo(1230.0);
    }
}
