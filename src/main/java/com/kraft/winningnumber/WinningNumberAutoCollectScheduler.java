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

    private final ExternalLottoProperties externalLottoProperties;
    private final WinningNumberCollectionService winningNumberCollectionService;

    public WinningNumberAutoCollectScheduler(ExternalLottoProperties externalLottoProperties,
                                             WinningNumberCollectionService winningNumberCollectionService) {
        this.externalLottoProperties = externalLottoProperties;
        this.winningNumberCollectionService = winningNumberCollectionService;
    }

    private static final int MAX_CATCH_UP_ROUNDS = 4;

    @Scheduled(cron = "${kraft.external-lotto.auto-collect-cron:0 30 21 * * SAT}", zone = "Asia/Seoul")
    @Scheduled(cron = "0 0 7 * * SUN", zone = "Asia/Seoul")
    @SchedulerLock(name = "collect-auto-latest", lockAtMostFor = "PT30M", lockAtLeastFor = "PT5M")
    public void collectLatestAutomatically() {
        if (!externalLottoProperties.enabled()) {
            log.info("자동 회차 수집을 건너뜁니다. 외부 수집 URL이 설정되지 않았습니다.");
            return;
        }

        try {
            var responses = winningNumberCollectionService.collectUpToLatest(MAX_CATCH_UP_ROUNDS);
            log.info("최신 회차 자동 수집 완료: collectedRounds={}",
                    responses.stream().map(WinningNumberResponse::round).toList());
        } catch (ApiException exception) {
            log.warn("최신 회차 자동 수집 실패: code={} message={}", exception.getCode(), exception.getMessage());
        } catch (Exception exception) {
            log.error("최신 회차 자동 수집 중 예외가 발생했습니다.", exception);
        }
    }
}
