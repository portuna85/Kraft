package com.kraft.winningnumber;

import java.util.List;

public record WinningNumberListResponse(
        List<WinningNumberResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
