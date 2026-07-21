package com.kraft.statistics;

import com.kraft.common.lotto.LottoNumbers;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AnalysisRequest(
        @NotNull @LottoNumbers
        List<Integer> numbers
) {
}
