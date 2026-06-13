package com.kraft.saved;

import java.time.OffsetDateTime;
import java.util.List;

public record SavedNumberResponse(
        long id,
        List<Integer> numbers,
        String label,
        String source,
        OffsetDateTime createdAt
) {
}
