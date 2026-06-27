package com.kraft.common.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String ROUNDS_LATEST = "rounds.latest";
    public static final String ROUNDS_LIST = "rounds.list";
    public static final String STATS_FREQUENCY = "stats.frequency";
    public static final String STATS_PATTERN = "stats.pattern";
    public static final String STATS_COMPANION = "stats.companion";

    @Bean
    CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.registerCustomCache(ROUNDS_LATEST, cache(5, TimeUnit.MINUTES, 1));
        manager.registerCustomCache(ROUNDS_LIST, cache(1, TimeUnit.HOURS, 20));
        manager.registerCustomCache(STATS_FREQUENCY, cache(10, TimeUnit.MINUTES, 4)); // null + 100 + 200 + 500
        manager.registerCustomCache(STATS_PATTERN, cache(10, TimeUnit.MINUTES, 10));
        manager.registerCustomCache(STATS_COMPANION, cache(10, TimeUnit.MINUTES, 1));
        return manager;
    }

    // P-4: Caffeine 캐시 적중률을 Prometheus에 노출
    @Bean
    ApplicationRunner cacheMicroMeterBinder(CacheManager cacheManager, MeterRegistry registry) {
        return args -> cacheManager.getCacheNames().forEach(name -> {
            @SuppressWarnings("unchecked")
            Cache<Object, Object> nativeCache = (Cache<Object, Object>)
                    Objects.requireNonNull(cacheManager.getCache(name)).getNativeCache();
            CaffeineCacheMetrics.monitor(registry, nativeCache, name);
        });
    }

    private static Cache<Object, Object> cache(long duration, TimeUnit unit, long maximumSize) {
        return Caffeine.newBuilder()
                .expireAfterWrite(duration, unit)
                .maximumSize(maximumSize)
                .recordStats()
                .build();
    }
}
