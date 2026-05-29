package com.kraft.lotto.support;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Cache;
import com.kraft.lotto.infra.config.KraftCacheProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(KraftCacheProperties cacheProperties,
                                     ObjectProvider<MeterRegistry> meterRegistryProvider) {
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable(SimpleMeterRegistry::new);
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.registerCustomCache("winningNumberFrequency", monitored(
                "winningNumberFrequency",
                buildCache(cacheProperties.winningNumberFrequency()),
                meterRegistry));
        manager.registerCustomCache("combinationPrizeHistory", monitored(
                "combinationPrizeHistory",
                buildCache(cacheProperties.combinationPrizeHistory()),
                meterRegistry));
        manager.registerCustomCache("winningFrequencySummary", monitored(
                "winningFrequencySummary",
                buildCache(cacheProperties.winningFrequencySummary()),
                meterRegistry));
        return manager;
    }

    private static Cache<Object, Object> buildCache(KraftCacheProperties.Spec spec) {
        return Caffeine.newBuilder()
                .expireAfterWrite(spec.ttlMinutes(), TimeUnit.MINUTES)
                .maximumSize(spec.maxSize())
                .recordStats()
                .build();
    }

    private static Cache<Object, Object> monitored(String cacheName,
                                                   Cache<Object, Object> cache,
                                                   MeterRegistry meterRegistry) {
        return CaffeineCacheMetrics.monitor(meterRegistry, cache, cacheName);
    }
}
