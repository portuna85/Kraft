package com.kraft.operationlog;

import java.time.OffsetDateTime;

public record WinningNumberOperationLogResponse(
        long id,
        String operationType,
        String executionStatus,
        Integer round,
        String sourceDetail,
        String message,
        String requestId,
        OffsetDateTime createdAt
) {
    public static WinningNumberOperationLogResponse from(WinningNumberOperationLog log) {
        return new WinningNumberOperationLogResponse(
                log.getId(),
                log.getOperationType().name(),
                log.getExecutionStatus().name(),
                log.getRound(),
                log.getSourceDetail(),
                log.getMessage(),
                log.getRequestId(),
                log.getCreatedAt()
        );
    }
}
