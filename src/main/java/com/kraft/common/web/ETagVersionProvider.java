package com.kraft.common.web;

import com.kraft.winningnumber.WinningNumberRepository;
import com.kraft.winningnumber.WinningNumbersCollectedEvent;
import jakarta.annotation.PostConstruct;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 캐싱 가능한 엔드포인트의 ETag를 도메인 버전(회차 번호)에서 파생한다.
 * 과거 회차 상세는 보정 후에도 값이 바뀌어야 하므로 MD5 폴백(바디 해시)에 맡기고,
 * 변경 가능 경로는 최신 회차 번호 + 단조 증가 성분으로 계산한다.
 */
@Component
public class ETagVersionProvider {

    private static final String UNKNOWN = "\"round-unknown\"";
    // 회차 번호만으로는 보정을 표현할 수 없는 경로 — 항상 MD5 폴백(응답 바디 해시)을 쓰도록 강제한다.
    private static final Set<String> MD5_FALLBACK_PATHS = Set.of(
            "/api/v1/rounds/freshness",
            "/api/v1/status/incidents"
    );

    private final AtomicReference<String> mutableETag = new AtomicReference<>(UNKNOWN);
    // 과거 회차를 재수집해도 mutableETag가 그 시절 값으로 회귀하지 않도록 단조 증가시키는 성분.
    private final AtomicLong bump = new AtomicLong();
    private final WinningNumberRepository winningNumberRepository;

    public ETagVersionProvider(WinningNumberRepository winningNumberRepository) {
        this.winningNumberRepository = winningNumberRepository;
    }

    @PostConstruct
    void init() {
        winningNumberRepository.findTopByOrderByRoundDesc()
                .ifPresent(wn -> mutableETag.set(formatEtag(wn.getRound())));
    }

    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onCollected(WinningNumbersCollectedEvent event) {
        if (event.dataChanged()) {
            int latest = winningNumberRepository.findTopByOrderByRoundDesc()
                    .map(wn -> wn.getRound())
                    .orElse(event.round());
            mutableETag.set(formatEtag(latest));
        }
    }

    private String formatEtag(int latestRound) {
        return "\"round-%d-b%d\"".formatted(latestRound, bump.incrementAndGet());
    }

    /**
     * 경로에 맞는 ETag를 반환한다. null 반환 시 호출자가 MD5 폴백(바디 해시)을 적용해야 한다.
     */
    public String etagForPath(String requestPath) {
        if (MD5_FALLBACK_PATHS.contains(requestPath)) {
            return null;
        }
        String version = mutableETag.get();
        return UNKNOWN.equals(version) ? null : version;
    }
}
