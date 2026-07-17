package com.kraft.winningnumber;

import com.kraft.common.config.ExternalLottoProperties;
import com.kraft.common.error.ApiException;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WinningNumberAutoCollectScheduler {

    private static final Logger log = LoggerFactory.getLogger(WinningNumberAutoCollectScheduler.class);

    /** 무한정 커질 수 없도록 두는 상한 — 실무상 이만큼 뒤처지는 일은 없고, 그런 경우는 관리자 백필로 처리한다. */
    private static final int MAX_CATCH_UP_ROUNDS_CAP = 60;

    private final ExternalLottoProperties externalLottoProperties;
    private final WinningNumberCollectionService winningNumberCollectionService;
    private final LottoFreshnessMetrics freshnessMetrics;

    public WinningNumberAutoCollectScheduler(ExternalLottoProperties externalLottoProperties,
                                             WinningNumberCollectionService winningNumberCollectionService,
                                             LottoFreshnessMetrics freshnessMetrics) {
        this.externalLottoProperties = externalLottoProperties;
        this.winningNumberCollectionService = winningNumberCollectionService;
        this.freshnessMetrics = freshnessMetrics;
    }

    @Scheduled(cron = "${kraft.external-lotto.auto-collect-cron:0 30/15 21-23 * * SAT}", zone = "Asia/Seoul")
    @Scheduled(cron = "0 0 7 * * SUN", zone = "Asia/Seoul")
    @SchedulerLock(name = "collect-auto-latest", lockAtMostFor = "PT30M", lockAtLeastFor = "PT5M")
    public void collectLatestAutomatically() {
        if (!externalLottoProperties.enabled()) {
            log.info("자동 회차 수집을 건너뜁니다. 외부 수집 URL이 설정되지 않았습니다.");
            return;
        }

        try {
            int maxRounds = catchUpRoundsFromGap();
            var responses = winningNumberCollectionService.collectUpToLatest(maxRounds);
            log.info("최신 회차 자동 수집 완료: collectedRounds={}",
                    responses.stream().map(WinningNumberResponse::round).toList());
        } catch (ApiException exception) {
            log.warn("최신 회차 자동 수집 실패: code={} message={}", exception.getCode(), exception.getMessage());
        } catch (Exception exception) {
            log.error("최신 회차 자동 수집 중 예외가 발생했습니다.", exception);
        }
    }

    /**
     * 이미 최신이면 첫 fetch가 즉시 LOTTO_SOURCE_ROUND_NOT_FOUND로 끝나므로 상한을 넉넉히 잡아도
     * 비용은 슬롯당 외부 호출 1회다. 누락 주수(gap)만큼 시도해 일시 장애로 여러 주가 밀려도
     * 자동으로 따라잡는다 — 기존 고정 상한 4는 4주 이상 누락되면 회복이 지연됐다.
     */
    private int catchUpRoundsFromGap() {
        var snapshot = freshnessMetrics.snapshot();
        long gap = (long) (snapshot.expectedLatestRound() - snapshot.latestRound()) + 1;
        return (int) Math.clamp(gap, 1, MAX_CATCH_UP_ROUNDS_CAP);
    }
}
