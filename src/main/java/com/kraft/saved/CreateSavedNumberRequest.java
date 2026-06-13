package com.kraft.saved;

import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateSavedNumberRequest(
        List<Integer> numbers,
        @Size(max = 100) String label,
        @Size(max = 30) String source
) {
}
