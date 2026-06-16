package com.kraft.winningnumber;

import com.kraft.common.error.ApiException;
import com.kraft.operationlog.WinningNumberOperationLogService;
import com.kraft.operationlog.WinningNumberOperationType;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WinningNumberCollectionService {

    private static final Logger log = LoggerFactory.getLogger(WinningNumberCollectionService.class);

    private final WinningNumberRepository winningNumberRepository;
    private final ExternalWinningNumberFetchClient externalWinningNumberFetchClient;
    private final WinningNumberCommandService winningNumberCommandService;
    private final WinningNumberOperationLogService winningNumberOperationLogService;
    private final ApplicationEventPublisher eventPublisher;

    public WinningNumberCollectionService(WinningNumberRepository winningNumberRepository,
                                          ExternalWinningNumberFetchClient externalWinningNumberFetchClient,
                                          WinningNumberCommandService winningNumberCommandService,
                                          WinningNumberOperationLogService winningNumberOperationLogService,
                                          ApplicationEventPublisher eventPublisher) {
        this.winningNumberRepository = winningNumberRepository;
        this.externalWinningNumberFetchClient = externalWinningNumberFetchClient;
        this.winningNumberCommandService = winningNumberCommandService;
        this.winningNumberOperationLogService = winningNumberOperationLogService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 전체 수집(백필) 완료 후 1회만 호출한다. 회차당 이벤트 폭주를 피하기 위함이다.
     * {@code @Transactional} 경계 안에서 발행해야 AFTER_COMMIT 리스너(ISR 재검증)도 동작한다.
     */
    public void publishBulkCollected(int latestRound) {
        log.info("전체 수집 완료 이벤트 발행: latestRound={}", latestRound);
        eventPublisher.publishEvent(new WinningNumbersCollectedEvent(latestRound, true));
    }

    public WinningNumberResponse collectLatest() {
        Optional<WinningNumber> latest = winningNumberRepository.findTopByOrderByRoundDesc();
        int nextRound = latest.map(winningNumber -> winningNumber.getRound() + 1).orElse(1);
        log.info("최신 회차 수집 시작: nextRound={}", nextRound);
        try {
            return collectFetchedRound(nextRound);
        } catch (RuntimeException exception) {
            if (isEndOfData(exception) && latest.isPresent()) {
                WinningNumber latestRound = latest.get();
                winningNumberOperationLogService.logSuccess(
                        WinningNumberOperationType.EXTERNAL_COLLECT,
                        latestRound.getRound(),
                        "external-source",
                        "다음 회차가 아직 공개되지 않았습니다. 이미 최신 회차까지 수집되어 있습니다."
                );
                log.info("최신 회차 수집 생략: latestRound={} nextRound={} reason={}",
                        latestRound.getRound(), nextRound, exception.getMessage());
                return WinningNumberResponse.from(latestRound);
            }
            winningNumberOperationLogService.logFailure(
                    WinningNumberOperationType.EXTERNAL_COLLECT,
                    nextRound,
                    "external-source",
                    exception.getMessage()
            );
            throw exception;
        }
    }

    public WinningNumberResponse collectRound(int round) {
        log.info("회차 수집 시작: round={}", round);
        try {
            return collectFetchedRound(round);
        } catch (RuntimeException exception) {
            winningNumberOperationLogService.logFailure(
                    WinningNumberOperationType.EXTERNAL_COLLECT,
                    round,
                    "external-source",
                    exception.getMessage()
            );
            throw exception;
        }
    }

    private WinningNumberResponse collectFetchedRound(int round) {
        WinningNumberUpsertRequest request = externalWinningNumberFetchClient.fetchRound(round);
        WinningNumberUpsertResult result = winningNumberCommandService.upsertWithResult(request);
        WinningNumberResponse response = result.response();
        winningNumberOperationLogService.logSuccess(
                WinningNumberOperationType.EXTERNAL_COLLECT,
                response.round(),
                "external-source",
                "외부 회차 수집 및 저장에 성공했습니다."
        );
        log.info("회차 수집 완료: round={} drawDate={} changed={}", response.round(), response.drawDate(), result.changed());
        eventPublisher.publishEvent(new WinningNumbersCollectedEvent(response.round(), result.changed()));
        return response;
    }

    private boolean isEndOfData(RuntimeException exception) {
        return exception instanceof ApiException apiException
                && ("LOTTO_SOURCE_ROUND_NOT_FOUND".equals(apiException.getCode())
                || "LOTTO_SOURCE_INVALID".equals(apiException.getCode()));
    }
}
