package com.kraft.winningnumber;

import com.kraft.common.config.RevalidateProperties;
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
            "/", "/latest", "/rounds", "/frequency", "/stats", "/companion"
    );

    private final RevalidateProperties revalidateProperties;
    private final RestClient restClient;

    public RevalidateWebhookListener(RevalidateProperties revalidateProperties) {
        this.revalidateProperties = revalidateProperties;
        var factory = new JdkClientHttpRequestFactory(
                java.net.http.HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(2))
                        .build()
        );
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCollected(WinningNumbersCollectedEvent event) {
        if (!event.dataChanged() || !revalidateProperties.enabled()) {
            return;
        }
        try {
            String url = revalidateProperties.webUrl() + "/api/revalidate";
            restClient.post()
                    .uri(url)
                    .header("X-Revalidate-Secret", revalidateProperties.secret())
                    .body(Map.of("paths", REVALIDATE_PATHS))
                    .retrieve()
                    .toBodilessEntity();
            log.info("ISR revalidation 요청 완료: paths={}", REVALIDATE_PATHS);
        } catch (Exception e) {
            log.warn("ISR revalidation 요청 실패 (무시): {}", e.getMessage());
        }
    }
}
