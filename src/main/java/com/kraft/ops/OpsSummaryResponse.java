package com.kraft.ops;

import java.time.ZonedDateTime;

public record OpsSummaryResponse(
        String service,
        String timezone,
        String status,
        Integer latestRound,
        String latestDrawDate,
        ZonedDateTime checkedAt,
        boolean fresh
) {
}
