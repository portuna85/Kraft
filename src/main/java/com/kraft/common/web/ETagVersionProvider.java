package com.kraft.common.web;

import com.kraft.winningnumber.WinningNumberRepository;
import com.kraft.winningnumber.WinningNumbersCollectedEvent;
import jakarta.annotation.PostConstruct;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 캐싱 가능한 엔드포인트의 ETag를 도메인 버전(회차 번호)에서 파생한다.
 * 불변 회차 경로는 URL에서 직접 추출, 변경 가능 경로는 최신 회차 번호로 계산한다.
 */
@Component
public class ETagVersionProvider {

    private static final Pattern HISTORICAL_ROUND = Pattern.compile("^/api/v1/rounds/(\\d+)$");
    private static final String UNKNOWN = "\"round-unknown\"";
    // 회차 번호와 무관하게 콘텐츠가 바뀔 수 있는 경로 — 회차 기반 ETag를 적용하면 최대 1주간 stale 304가 발생하므로
    // 항상 MD5 폴백(응답 바디 해시)을 쓰도록 강제한다.
    private static final Set<String> MD5_FALLBACK_PATHS = Set.of(
            "/api/v1/rounds/freshness",
            "/api/v1/status/incidents"
    );

    private final AtomicReference<String> mutableETag = new AtomicReference<>(UNKNOWN);
    private final WinningNumberRepository winningNumberRepository;

    public ETagVersionProvider(WinningNumberRepository winningNumberRepository) {
        this.winningNumberRepository = winningNumberRepository;
    }

    @PostConstruct
    void init() {
        winningNumberRepository.findTopByOrderByRoundDesc()
                .ifPresent(wn -> mutableETag.set("\"round-" + wn.getRound() + "\""));
    }

    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onCollected(WinningNumbersCollectedEvent event) {
        if (event.dataChanged()) {
            mutableETag.set("\"round-" + event.round() + "\"");
        }
    }

    /**
     * 경로에 맞는 ETag를 반환한다. 불변 경로는 즉시 계산, 나머지는 현재 회차 기반.
     * null 반환 시 호출자가 MD5 폴백을 적용해야 한다.
     */
    public String etagForPath(String requestPath) {
        var matcher = HISTORICAL_ROUND.matcher(requestPath);
        if (matcher.matches()) {
            return "\"round-" + matcher.group(1) + "\"";
        }
        if (MD5_FALLBACK_PATHS.contains(requestPath)) {
            return null;
        }
        String version = mutableETag.get();
        return UNKNOWN.equals(version) ? null : version;
    }
}
