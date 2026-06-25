package com.kraft.saved;

import java.time.LocalDate;
import java.util.List;

public record SavedNumberMatchResult(
        SavedNumberResponse savedNumber,
        int round,
        LocalDate drawDate,
        List<Integer> drawNumbers,
        int bonusNumber,
        int matchedCount,
        boolean bonusMatch,
        String prizeTier
) {
}
