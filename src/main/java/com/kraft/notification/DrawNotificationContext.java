package com.kraft.notification;

import java.time.LocalDate;
import java.util.List;

public record DrawNotificationContext(
        int round,
        LocalDate drawDate,
        List<Integer> winningNumbers,
        int bonusNumber,
        List<SavedEntryResult> savedEntries
) {
    public record SavedEntryResult(
            List<Integer> numbers,
            String label,
            int matchCount,
            boolean bonusMatch,
            String rank
    ) {
    }
}
