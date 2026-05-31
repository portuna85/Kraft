package com.kraft.lotto.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.benmanes.caffeine.cache.Cache;
import com.kraft.lotto.infra.config.KraftCacheProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@DisplayName("캐시 설정")
class CacheConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(CacheConfig.class, CachePropsConfig.class)
            .withPropertyValues(
                    "kraft.cache.winning-number-frequency.ttl-minutes=5",
                    "kraft.cache.winning-number-frequency.max-size=1000",
                    "kraft.cache.combination-prize-history.ttl-minutes=10",
                    "kraft.cache.combination-prize-history.max-size=2000",
                    "kraft.cache.winning-frequency-summary.ttl-minutes=5",
                    "kraft.cache.winning-frequency-summary.max-size=1000"
            );

    @Test
    @DisplayName("모든 캐시가 CacheManager에 등록된다")
    void registersAllCaches() {
        runner.run(context -> {
            CacheManager cacheManager = context.getBean(CacheManager.class);
            assertThat(cacheManager.getCache("winningNumberFrequency")).isNotNull();
            assertThat(cacheManager.getCache("combinationPrizeHistory")).isNotNull();
            assertThat(cacheManager.getCache("winningFrequencySummary")).isNotNull();
            assertThat(cacheManager.getCache("winningNumberFrequencyPeriod")).isNotNull();
            assertThat(cacheManager.getCache("patternStats")).isNotNull();
            assertThat(cacheManager.getCache("companionNumbers")).isNotNull();
        });
    }

    @Test
    @DisplayName("캐시는 통계 수집(recordStats)이 활성화된 Caffeine 캐시로 생성된다")
    void cacheHasStatsRecordingEnabled() {
        runner.run(context -> {
            CacheManager cacheManager = context.getBean(CacheManager.class);
            CaffeineCache caffeineCache = (CaffeineCache) cacheManager.getCache("winningNumberFrequency");
            Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();

            nativeCache.get("key", k -> "value");

            assertThat(nativeCache.stats().requestCount()).isPositive();
        });
    }

    @Configuration
    static class CachePropsConfig {
        @Bean
        KraftCacheProperties kraftCacheProperties() {
            return new KraftCacheProperties(
                    new KraftCacheProperties.Spec(5, 1000),
                    new KraftCacheProperties.Spec(10, 2000),
                    new KraftCacheProperties.Spec(5, 1000),
                    new KraftCacheProperties.Spec(10, 4),
                    new KraftCacheProperties.Spec(30, 1),
                    new KraftCacheProperties.Spec(30, 45)
            );
        }
    }
}
