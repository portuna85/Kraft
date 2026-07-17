package com.kraft.statistics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * 통계 summary가 실제 최신 회차를 얼마나 반영했는지를 gauge로 노출한다.
 * {@code kraft_lotto_latest_round}(LottoFreshnessMetrics)와 이 값의 차이가 곧
 * "보정·수집 이벤트 발행 실패로 통계가 뒤처진 정도"다.
 */
@Component
public class StatisticsFreshnessMetrics {

    private final FrequencySummaryRepository frequencySummaryRepository;

    public StatisticsFreshnessMetrics(MeterRegistry registry, FrequencySummaryRepository frequencySummaryRepository) {
        this.frequencySummaryRepository = frequencySummaryRepository;
        Gauge.builder("kraft_lotto_statistics_projected_round", this, StatisticsFreshnessMetrics::projectedRound)
                .description("통계 summary가 반영한 최신 회차")
                .register(registry);
    }

    private double projectedRound() {
        return frequencySummaryRepository.findMaxLastRound();
    }
}
