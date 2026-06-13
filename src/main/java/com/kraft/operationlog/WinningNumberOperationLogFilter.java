package com.kraft.operationlog;

import java.time.OffsetDateTime;

public record WinningNumberOperationLogFilter(
        WinningNumberOperationType operationType,
        WinningNumberOperationStatus executionStatus,
        Integer round,
        OffsetDateTime createdFrom,
        OffsetDateTime createdToExclusive
) {
}
