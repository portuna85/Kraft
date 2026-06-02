package com.kraft.lotto.support;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Cache;
import com.kraft.lotto.infra.config.KraftCacheProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(KraftCacheProperties cacheProperties, MeterRegistry meterRegistry) {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.registerCustomCache("winningNumberFrequency",
                buildCache("winningNumberFrequency", cacheProperties.winningNumberFrequency(), meterRegistry));
        manager.registerCustomCache("combinationPrizeHistory",
                buildCache("combinationPrizeHistory", cacheProperties.combinationPrizeHistory(), meterRegistry));
        manager.registerCustomCache("winningFrequencySummary",
                buildCache("winningFrequencySummary", cacheProperties.winningFrequencySummary(), meterRegistry));
        manager.registerCustomCache("winningNumberFrequencyPeriod",
                buildCache("winningNumberFrequencyPeriod", cacheProperties.winningNumberFrequencyPeriod(), meterRegistry));
        manager.registerCustomCache("patternStats",
                buildCache("patternStats", cacheProperties.patternStats(), meterRegistry));
        manager.registerCustomCache("companionNumbers",
                buildCache("companionNumbers", cacheProperties.companionNumbers(), meterRegistry));
        return manager;
    }

    private static Cache<Object, Object> buildCache(String cacheName,
                                                    KraftCacheProperties.Spec spec,
                                                    MeterRegistry meterRegistry) {
        Cache<Object, Object> cache = Caffeine.newBuilder()
                .expireAfterWrite(spec.ttlMinutes(), TimeUnit.MINUTES)
                .maximumSize(spec.maxSize())
                .recordStats()
                .build();
        CaffeineCacheMetrics.monitor(meterRegistry, cache, cacheName);
        return cache;
    }
}
