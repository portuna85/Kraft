package com.kraft.lotto.feature.statistics.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("로또 이론적 조합 분포")
class LottoTheoreticalDistributionTest {

    @Test
    @DisplayName("홀짝 비율의 합은 100%이다")
    void oddEvenPercentsSumTo100() {
        double total = 0;
        for (int k = 0; k <= 6; k++) {
            total += LottoTheoreticalDistribution.oddEvenPercent(k);
        }
        assertThat(total).isCloseTo(100.0, within(0.01));
    }

    @Test
    @DisplayName("3홀 3짝이 가장 높은 비율이다")
    void threeOddThreeEvenIsHighest() {
        double max = 0;
        int maxK = -1;
        for (int k = 0; k <= 6; k++) {
            double pct = LottoTheoreticalDistribution.oddEvenPercent(k);
            if (pct > max) {
                max = pct;
                maxK = k;
            }
        }
        assertThat(maxK).isEqualTo(3);
    }

    @Test
    @DisplayName("합산 버킷 비율의 합은 100%이다")
    void sumBucketPercentsSumTo100() {
        Map<Integer, Double> percents = LottoTheoreticalDistribution.sumBucketPercents(10);
        double total = percents.values().stream().mapToDouble(Double::doubleValue).sum();
        assertThat(total).isCloseTo(100.0, within(0.01));
    }

    @Test
    @DisplayName("합산 분포는 130~139 구간 근처에서 피크를 이룬다")
    void sumDistributionPeaksAround130to150() {
        Map<Integer, Double> percents = LottoTheoreticalDistribution.sumBucketPercents(10);
        double max = 0;
        int peakBucket = -1;
        for (Map.Entry<Integer, Double> e : percents.entrySet()) {
            if (e.getValue() > max) {
                max = e.getValue();
                peakBucket = e.getKey();
            }
        }
        // 기댓값 = 6 * 23 = 138이므로 피크는 130~139 또는 이웃 구간
        assertThat(peakBucket).isBetween(120, 150);
    }

    @Test
    @DisplayName("45개 중 6개를 고르는 조합은 8,145,060개이다")
    void totalCombinationsIsCorrect() {
        assertThat(LottoTheoreticalDistribution.TOTAL_COMBINATIONS).isEqualTo(8_145_060L);
    }
}
