package com.kraft.lotto.support;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Cache;
import com.kraft.lotto.infra.config.KraftCacheProperties;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(KraftCacheProperties cacheProperties) {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.registerCustomCache("winningNumberFrequency",
                buildCache(cacheProperties.winningNumberFrequency()));
        manager.registerCustomCache("combinationPrizeHistory",
                buildCache(cacheProperties.combinationPrizeHistory()));
        manager.registerCustomCache("winningFrequencySummary",
                buildCache(cacheProperties.winningFrequencySummary()));
        manager.registerCustomCache("winningNumberFrequencyPeriod",
                buildCache(cacheProperties.winningNumberFrequencyPeriod()));
        manager.registerCustomCache("patternStats",
                buildCache(cacheProperties.patternStats()));
        manager.registerCustomCache("companionNumbers",
                buildCache(cacheProperties.companionNumbers()));
        return manager;
    }

    private static Cache<Object, Object> buildCache(KraftCacheProperties.Spec spec) {
        return Caffeine.newBuilder()
                .expireAfterWrite(spec.ttlMinutes(), TimeUnit.MINUTES)
                .maximumSize(spec.maxSize())
                .recordStats()
                .build();
    }
}
