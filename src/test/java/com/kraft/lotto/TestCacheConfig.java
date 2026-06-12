package com.kraft.lotto;

import com.kraft.lotto.infra.config.KraftAdProperties;
import com.kraft.lotto.infra.config.KraftWebProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;

@TestConfiguration
@EnableConfigurationProperties({KraftAdProperties.class, KraftWebProperties.class})
public class TestCacheConfig {

    @Bean
    CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("winningNumberFrequency", "combinationPrizeHistory");
    }
}
