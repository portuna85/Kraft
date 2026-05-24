package com.kraft.lotto.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.kraft.lotto.infra.config.KraftCacheProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@DisplayName("캐시 설정")
class CacheConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(CacheConfig.class, MeterConfig.class, CachePropsConfig.class)
            .withPropertyValues(
                    "kraft.cache.winning-number-frequency.ttl-minutes=5",
                    "kraft.cache.winning-number-frequency.max-size=1000",
                    "kraft.cache.combination-prize-history.ttl-minutes=10",
                    "kraft.cache.combination-prize-history.max-size=2000",
                    "kraft.cache.winning-frequency-summary.ttl-minutes=5",
                    "kraft.cache.winning-frequency-summary.max-size=1000"
            );

    @Test
    @DisplayName("Caffeine 캐시 지표를 MeterRegistry에 바인딩한다")
    void bindsCaffeineCacheMetrics() {
        runner.run(context -> {
            CacheManager cacheManager = context.getBean(CacheManager.class);
            SimpleMeterRegistry registry = context.getBean(SimpleMeterRegistry.class);

            cacheManager.getCache("winningNumberFrequency").get("freq", () -> "value");
            cacheManager.getCache("combinationPrizeHistory").get("combo", () -> "value");
            cacheManager.getCache("winningFrequencySummary").get("summary", () -> "value");

            assertThat(registry.find("cache.gets").tag("cache", "winningNumberFrequency").meters()).isNotEmpty();
            assertThat(registry.find("cache.gets").tag("cache", "combinationPrizeHistory").meters()).isNotEmpty();
            assertThat(registry.find("cache.gets").tag("cache", "winningFrequencySummary").meters()).isNotEmpty();
        });
    }

    @Configuration
    static class MeterConfig {
        @Bean
        SimpleMeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    @Configuration
    static class CachePropsConfig {
        @Bean
        KraftCacheProperties kraftCacheProperties() {
            return new KraftCacheProperties(
                    new KraftCacheProperties.Spec(5, 1000),
                    new KraftCacheProperties.Spec(10, 2000),
                    new KraftCacheProperties.Spec(5, 1000)
            );
        }
    }
}
