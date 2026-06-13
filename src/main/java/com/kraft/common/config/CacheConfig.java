package com.kraft.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
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
    public static final String RECOMMEND_RULES = "recommend.rules";

    @Bean
    CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.registerCustomCache(ROUNDS_LATEST,
                Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).maximumSize(1).build());
        manager.registerCustomCache(ROUNDS_LIST,
                Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).maximumSize(20).build());
        manager.registerCustomCache(STATS_FREQUENCY,
                Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(1).build());
        manager.registerCustomCache(STATS_PATTERN,
                Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(10).build());
        manager.registerCustomCache(STATS_COMPANION,
                Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(1).build());
        manager.registerCustomCache(RECOMMEND_RULES,
                Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).maximumSize(1).build());
        return manager;
    }
}
