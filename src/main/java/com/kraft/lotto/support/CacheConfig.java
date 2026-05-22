package com.kraft.lotto.support;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Cache;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(ObjectProvider<MeterRegistry> meterRegistryProvider) {
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.registerCustomCache("winningNumberFrequency", monitored(
                "winningNumberFrequency",
                Caffeine.newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(1)
                        .recordStats()
                        .build(),
                meterRegistry));
        manager.registerCustomCache("combinationPrizeHistory", monitored(
                "combinationPrizeHistory",
                Caffeine.newBuilder()
                        .expireAfterWrite(30, TimeUnit.MINUTES)
                        .maximumSize(200)
                        .recordStats()
                        .build(),
                meterRegistry));
        manager.registerCustomCache("winningFrequencySummary", monitored(
                "winningFrequencySummary",
                Caffeine.newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(1)
                        .recordStats()
                        .build(),
                meterRegistry));
        return manager;
    }

    private static Cache<Object, Object> monitored(String cacheName,
                                                   Cache<Object, Object> cache,
                                                   MeterRegistry meterRegistry) {
        if (meterRegistry == null) {
            return cache;
        }
        return CaffeineCacheMetrics.monitor(meterRegistry, cache, cacheName);
    }
}
