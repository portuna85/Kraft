package com.kraft.ops;

import com.kraft.common.config.OpsProperties;
import com.kraft.common.error.ApiException;
import com.kraft.operationlog.WinningNumberOperationLogFilter;
import com.kraft.operationlog.WinningNumberOperationLogPageResponse;
import com.kraft.operationlog.WinningNumberOperationLogService;
import com.kraft.operationlog.WinningNumberOperationStatus;
import com.kraft.operationlog.WinningNumberOperationType;
import com.kraft.winningnumber.LottoDrawScheduleCalculator;
import com.kraft.winningnumber.WinningNumberCollectionService;
import com.kraft.winningnumber.WinningNumberCommandService;
import com.kraft.winningnumber.WinningNumberRepository;
import com.kraft.winningnumber.WinningNumberResponse;
import com.kraft.winningnumber.WinningNumberUpsertRequest;
import com.kraft.common.web.RequestIdFilter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OpsService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Logger log = LoggerFactory.getLogger(OpsService.class);

    private final WinningNumberRepository winningNumberRepository;
    private final WinningNumberCommandService winningNumberCommandService;
    private final WinningNumberCollectionService winningNumberCollectionService;
    private final WinningNumberOperationLogService winningNumberOperationLogService;
    private final OpsProperties opsProperties;
    private final Clock clock;
    private final LottoDrawScheduleCalculator drawScheduleCalculator;

    public OpsService(WinningNumberRepository winningNumberRepository,
                      WinningNumberCommandService winningNumberCommandService,
                      WinningNumberCollectionService winningNumberCollectionService,
                      WinningNumberOperationLogService winningNumberOperationLogService,
                      OpsProperties opsProperties,
                      Clock clock,
                      LottoDrawScheduleCalculator drawScheduleCalculator) {
        this.winningNumberRepository = winningNumberRepository;
        this.winningNumberCommandService = winningNumberCommandService;
        this.winningNumberCollectionService = winningNumberCollectionService;
        this.winningNumberOperationLogService = winningNumberOperationLogService;
        this.opsProperties = opsProperties;
        this.clock = clock;
        this.drawScheduleCalculator = drawScheduleCalculator;
    }

    public OpsSummaryResponse getSummary(String token) {
        validateToken(token);

        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(KST);
        OpsSummaryResponse response = winningNumberRepository.findTopByOrderByRoundDesc()
                .map(latest -> new OpsSummaryResponse(
                        "kraft-lotto",
                        KST.getId(),
                        "정상",
                        latest.getRound(),
                        latest.getDrawDate().toString(),
                        now,
                        isFresh(latest.getDrawDate(), now)
                ))
                .orElseGet(() -> new OpsSummaryResponse(
                        "kraft-lotto",
                        KST.getId(),
                        "데이터 없음",
                        null,
                        null,
                        now,
                        false
                ));
        log.info("운영 상태 조회 완료: latestRound={} fresh={}", response.latestRound(), response.fresh());
        return response;
    }

    public WinningNumberOperationLogPageResponse getRecentOperationLogs(String token,
                                                                        int page,
                                                                        int size,
                                                                        String operationType,
                                                                        String executionStatus,
                                                                        Integer round,
                                                                        String from,
                                                                        String to) {
        validateToken(token);
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.clamp(size, 1, 100);
        WinningNumberOperationLogFilter filter = new WinningNumberOperationLogFilter(
                parseOperationType(operationType),
                parseExecutionStatus(executionStatus),
                round,
                parseFromDate(from),
                parseToDateExclusive(to)
        );
        log.info("운영 작업 로그 조회: page={} size={} operationType={} executionStatus={} round={} from={} toExclusive={}",
                normalizedPage,
                normalizedSize,
                filter.operationType(),
                filter.executionStatus(),
                filter.round(),
                filter.createdFrom(),
                filter.createdToExclusive());
        return winningNumberOperationLogService.getRecentLogs(normalizedPage, normalizedSize, filter);
    }

    @Transactional
    public WinningNumberResponse upsertWinningNumber(String token, WinningNumberUpsertRequest request) {
        validateToken(token);
        String caller = callerDetail();
        log.info("운영 수동 회차 저장 시작: round={} drawDate={} caller={}", request.round(), request.drawDate(), caller);
        try {
            WinningNumberResponse response = winningNumberCommandService.upsert(request);
            winningNumberOperationLogService.logSuccess(
                    WinningNumberOperationType.MANUAL_UPSERT,
                    response.round(),
                    caller,
                    "운영 수동 회차 저장에 성공했습니다."
            );
            log.info("운영 수동 회차 저장 완료: round={} caller={}", response.round(), caller);
            return response;
        } catch (RuntimeException exception) {
            winningNumberOperationLogService.logFailure(
                    WinningNumberOperationType.MANUAL_UPSERT,
                    request.round(),
                    caller,
                    exception.getMessage()
            );
            throw exception;
        }
    }

    // @Transactional을 두지 않는다 — collectLatest() 내부의 외부 HTTP fetch(fetchRound)가
    // 트랜잭션 밖에서 실행돼야 커넥션을 길게 점유하지 않는다(P1-3). 실제 저장은
    // WinningNumberCommandService가 호출당 짧은 트랜잭션으로 처리한다.
    public WinningNumberResponse collectLatestWinningNumber(String token) {
        validateToken(token);
        log.info("운영 최신 회차 수집 요청: caller={}", callerDetail());
        return winningNumberCollectionService.collectLatest();
    }

    public WinningNumberResponse collectWinningNumber(String token, int round) {
        validateToken(token);
        if (round < 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ROUND", "회차는 1 이상이어야 합니다.");
        }
        log.info("운영 특정 회차 수집 요청: round={} caller={}", round, callerDetail());
        return winningNumberCollectionService.collectRound(round);
    }

    private static String callerDetail() {
        String ip = MDC.get(RequestIdFilter.MDC_CLIENT_IP);
        String requestId = MDC.get(RequestIdFilter.MDC_KEY);
        return "ops-api ip=" + (ip != null ? ip : "unknown") + " requestId=" + (requestId != null ? requestId : "none");
    }

    private void validateToken(String token) {
        String expected = opsProperties.token();
        if (expected == null || expected.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "OPS_DISABLED", "운영 API 토큰이 설정되지 않았습니다.");
        }
        if (token == null || !MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "OPS_UNAUTHORIZED", "운영 API 인증에 실패했습니다.");
        }
    }

    private boolean isFresh(LocalDate latestDrawDate, ZonedDateTime now) {
        LocalDate expected = drawScheduleCalculator.expectedLatestDrawDate(now);
        return !latestDrawDate.isBefore(expected);
    }

    private WinningNumberOperationType parseOperationType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return WinningNumberOperationType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_OPERATION_TYPE", "지원하지 않는 작업 유형입니다.");
        }
    }

    private WinningNumberOperationStatus parseExecutionStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return WinningNumberOperationStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_EXECUTION_STATUS", "지원하지 않는 실행 상태입니다.");
        }
    }

    private OffsetDateTime parseFromDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim())
                    .atStartOfDay(KST)
                    .toOffsetDateTime();
        } catch (RuntimeException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_FROM_DATE", "from 날짜 형식이 올바르지 않습니다. yyyy-MM-dd 형식을 사용하세요.");
        }
    }

    private OffsetDateTime parseToDateExclusive(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim())
                    .plusDays(1)
                    .atStartOfDay(KST)
                    .toOffsetDateTime();
        } catch (RuntimeException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TO_DATE", "to 날짜 형식이 올바르지 않습니다. yyyy-MM-dd 형식을 사용하세요.");
        }
    }
}
