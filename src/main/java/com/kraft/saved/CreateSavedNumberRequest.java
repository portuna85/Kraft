package com.kraft.saved;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateSavedNumberRequest(
        @NotNull @Size(min = 6, max = 6, message = "번호는 정확히 6개여야 합니다.")
        List<@NotNull @Min(1) @Max(45) Integer> numbers,
        @Size(max = 100) String label,
        @Size(max = 30) String source
) {
}
