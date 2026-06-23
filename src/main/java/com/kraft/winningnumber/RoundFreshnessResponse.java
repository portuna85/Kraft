package com.kraft.winningnumber;

import java.time.LocalDate;
import java.time.ZonedDateTime;

public record RoundFreshnessResponse(
        int latestRound,
        LocalDate latestDrawDate,
        boolean fresh,
        ZonedDateTime checkedAt
) {
}
