package com.kraft.winningnumber;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record WinningNumberUpsertRequest(
        @NotNull @Min(1) Integer round,
        @NotNull LocalDate drawDate,
        @NotNull List<Integer> numbers,
        @NotNull @Min(1) @Max(45) Integer bonusNumber,
        @NotNull @Min(0) Long firstPrizeAmount,
        @Min(0) Long secondPrize,
        @Min(0) Integer secondWinners,
        @Min(0) Long totalSales,
        @Min(0) Long firstAccumAmount
) {
}
