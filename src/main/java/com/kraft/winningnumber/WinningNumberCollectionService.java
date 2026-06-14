package com.kraft.winningnumber;

import com.kraft.operationlog.WinningNumberOperationLogService;
import com.kraft.operationlog.WinningNumberOperationType;
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

    public WinningNumberResponse collectLatest() {
        int nextRound = winningNumberRepository.findTopByOrderByRoundDesc()
                .map(winningNumber -> winningNumber.getRound() + 1)
                .orElse(1);
        log.info("최신 회차 수집 시작: nextRound={}", nextRound);
        return collectRound(nextRound);
    }

    public WinningNumberResponse collectRound(int round) {
        log.info("회차 수집 시작: round={}", round);
        try {
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
            eventPublisher.publishEvent(new WinningNumbersCollectedEvent(round, result.changed()));
            return response;
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
}
