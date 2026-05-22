package com.kraft.lotto.feature.recommend.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.kraft.lotto.feature.recommend.domain.ExclusionRule;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("perf")
@DisplayName("로또 추천기 성능 스모크 테스트")
class LottoRecommenderPerformanceSmokeTest {

    @Test
    @DisplayName("추천 경로가 스모크 임계값 내에 머문다")
    void recommendPathWithinThreshold() {
        List<ExclusionRule> noRules = List.of();
        LottoRecommender recommender = new LottoRecommender(noRules, new Random(42L), 20000);

        int iterations = 200;
        long startedAt = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            var combinations = recommender.recommend(10);
            assertThat(combinations).hasSize(10);
        }
        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000L;

        assertThat(elapsedMs).isLessThan(2000L);
    }
}
