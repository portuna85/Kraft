package com.kraft.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "eventTaskExecutor")
    ThreadPoolTaskExecutor eventTaskExecutor() {
        var ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(2);
        ex.setMaxPoolSize(4);
        ex.setQueueCapacity(50);
        ex.setThreadNamePrefix("kraft-evt-");
        ex.initialize();
        return ex;
    }

    /**
     * 전체 회차 수집(백필) 전용 단일 스레드 풀.
     * 수 분이 걸리는 장시간 작업이 이벤트 처리 풀을 점유하지 않도록 분리하고,
     * 단일 스레드 + 큐 1로 동시 백필 실행을 차단한다.
     */
    @Bean(name = "backfillTaskExecutor")
    ThreadPoolTaskExecutor backfillTaskExecutor() {
        var ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(1);
        ex.setMaxPoolSize(1);
        ex.setQueueCapacity(1);
        ex.setThreadNamePrefix("kraft-backfill-");
        ex.initialize();
        return ex;
    }
}
