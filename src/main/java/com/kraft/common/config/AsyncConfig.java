package com.kraft.common.config;

import java.util.concurrent.ThreadPoolExecutor;
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
        ex.setWaitForTasksToCompleteOnShutdown(true);
        ex.setAwaitTerminationSeconds(30);
        // 큐 포화 시 기본 AbortPolicy는 이벤트를 조용히 유실한다.
        // CallerRuns는 발행 스레드에서 동기 처리해 유실 없이 역압을 전파한다.
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
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
        ex.setWaitForTasksToCompleteOnShutdown(true);
        ex.setAwaitTerminationSeconds(60);
        // 큐 1 설계로 이미 실행 중인 백필이 있으면 두 번째 요청은 드롭한다(의도된 스로틀).
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        ex.initialize();
        return ex;
    }
}
