package com.kraft.saved;

import com.kraft.common.lotto.LottoNumbers;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateSavedNumberRequest(
        @NotNull @LottoNumbers
        List<Integer> numbers,
        @Size(max = 100) String label,
        @Size(max = 30) String source
) {
}
