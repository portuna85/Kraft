package com.kraft.winningnumber;

import com.kraft.common.config.RevalidateProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.web.client.RestClient;

/**
 * 수집 완료 이벤트 수신 → web 컨테이너에 on-demand ISR revalidation 요청.
 * 비동기 실행 — 실패해도 수집 트랜잭션에 영향 없음.
 */
@Component
public class RevalidateWebhookListener {

    private static final Logger log = LoggerFactory.getLogger(RevalidateWebhookListener.class);
    private static final List<String> REVALIDATE_PATHS = List.of(
            "/", "/rounds", "/frequency", "/stats", "/companion"
    );

    private final RevalidateProperties revalidateProperties;
    private final RestClient restClient;
    private final Counter revalidateFailureCounter;

    public RevalidateWebhookListener(RevalidateProperties revalidateProperties, MeterRegistry meterRegistry) {
        this.revalidateProperties = revalidateProperties;
        var factory = new JdkClientHttpRequestFactory(
                java.net.http.HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(2))
                        .build()
        );
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().requestFactory(factory).build();
        this.revalidateFailureCounter = Counter.builder("kraft_lotto_revalidate_failures_total")
                .description("ISR on-demand revalidation 요청 실패 횟수")
                .register(meterRegistry);
    }

    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCollected(WinningNumbersCollectedEvent event) {
        if (!event.dataChanged() || !revalidateProperties.enabled()) {
            return;
        }
        List<String> paths = revalidatePathsFor(event.round());
        List<String> tags = tagsFor(event.round());
        try {
            String url = revalidateProperties.webUrl() + "/api/revalidate";
            restClient.post()
                    .uri(url)
                    .header("X-Revalidate-Secret", revalidateProperties.secret())
                    .body(Map.of("paths", paths, "tags", tags))
                    .retrieve()
                    .toBodilessEntity();
            log.info("ISR revalidation 요청 완료: paths={} tags={}", paths, tags);
        } catch (Exception e) {
            revalidateFailureCounter.increment();
            log.warn("ISR revalidation 요청 실패 (무시): {}", e.getMessage());
        }
    }

    static List<String> revalidatePathsFor(int round) {
        List<String> paths = new java.util.ArrayList<>(REVALIDATE_PATHS);
        paths.add("/rounds/" + round);
        return List.copyOf(paths);
    }

    // 프론트 web/src/lib/revalidate.ts의 TAG_* 상수와 이름이 일치해야 한다.
    static List<String> tagsFor(int round) {
        return List.of("rounds:latest", "rounds:list", "stats:all", "rounds:detail:" + round);
    }
}
