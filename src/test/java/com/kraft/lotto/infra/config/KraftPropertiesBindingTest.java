package com.kraft.lotto.infra.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "KRAFT_DOTENV_ENABLED=false")
@ActiveProfiles("test")
@DisplayName("설정 프로퍼티 바인딩 테스트")
class KraftPropertiesBindingTest {

    @Autowired
    KraftApiProperties api;

    @Autowired
    KraftRecommendProperties recommend;

    @Autowired
    KraftCacheProperties cache;

    @Test
    @DisplayName("에이피아이 설정 프로퍼티를 바인딩한다")
    void bindsApiProperties() {
        assertThat(api.client()).isEqualTo("mock");
        assertThat(api.url()).isEqualTo("http://localhost");
        assertThat(api.connectTimeoutMs()).isEqualTo(2000);
        assertThat(api.readTimeoutMs()).isEqualTo(3000);
        assertThat(api.requestTimeoutMs()).isEqualTo(10000);
        assertThat(api.maxRetries()).isEqualTo(2);
        assertThat(api.retryBackoffMs()).isEqualTo(50);
        assertThat(api.mockLatestRound()).isIn(0, 1200);
    }

    @Test
    @DisplayName("추천 설정 프로퍼티를 바인딩한다")
    void bindsRecommendProperties() {
        assertThat(recommend.maxAttempts()).isEqualTo(1000);
        assertThat(recommend.rules().birthdayThreshold()).isEqualTo(31);
        assertThat(recommend.rules().longRunThreshold()).isEqualTo(5);
        assertThat(recommend.rules().decadeThreshold()).isEqualTo(5);
    }

    @Test
    @DisplayName("캐시 설정 프로퍼티를 바인딩한다")
    void bindsCacheProperties() {
        assertThat(cache.winningNumberFrequency().ttlMinutes()).isEqualTo(10);
        assertThat(cache.winningNumberFrequency().maxSize()).isEqualTo(1);
        assertThat(cache.combinationPrizeHistory().ttlMinutes()).isEqualTo(30);
        assertThat(cache.combinationPrizeHistory().maxSize()).isEqualTo(200);
        assertThat(cache.winningFrequencySummary().ttlMinutes()).isEqualTo(5);
        assertThat(cache.winningFrequencySummary().maxSize()).isEqualTo(1);
        assertThat(cache.winningNumberFrequencyPeriod().ttlMinutes()).isEqualTo(10);
        assertThat(cache.winningNumberFrequencyPeriod().maxSize()).isEqualTo(4);
        assertThat(cache.patternStats().ttlMinutes()).isEqualTo(30);
        assertThat(cache.patternStats().maxSize()).isEqualTo(1);
        assertThat(cache.companionNumbers().ttlMinutes()).isEqualTo(30);
        assertThat(cache.companionNumbers().maxSize()).isEqualTo(45);
    }
}
