package com.kraft.operationlog;

import java.util.List;

public record WinningNumberOperationLogPageResponse(
        List<WinningNumberOperationLogResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
