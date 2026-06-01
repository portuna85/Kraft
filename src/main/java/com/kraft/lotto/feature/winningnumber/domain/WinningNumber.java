package com.kraft.lotto.feature.winningnumber.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public record WinningNumber(
        int round,
        LocalDate drawDate,
        LottoCombination combination,
        int bonusNumber,
        long firstPrize,
        int firstWinners,
        long totalSales,
        long firstAccumAmount,
        long secondPrize,
        int secondWinners,
        String rawJson,
        LocalDateTime fetchedAt
) {

    public WinningNumber(int round,
                         LocalDate drawDate,
                         LottoCombination combination,
                         int bonusNumber,
                         long firstPrize,
                         int firstWinners,
                         long totalSales) {
        this(round, drawDate, combination, bonusNumber, firstPrize, firstWinners, totalSales, 0L, 0L, 0, null, null);
    }

    public WinningNumber(int round,
                         LocalDate drawDate,
                         LottoCombination combination,
                         int bonusNumber,
                         long firstPrize,
                         int firstWinners,
                         long totalSales,
                         long firstAccumAmount,
                         String rawJson,
                         LocalDateTime fetchedAt) {
        this(round, drawDate, combination, bonusNumber, firstPrize, firstWinners, totalSales, firstAccumAmount, 0L, 0, rawJson, fetchedAt);
    }

    public WinningNumber {
        if (round <= 0) {
            throw new IllegalArgumentException("회차는 0보다 커야 합니다: " + round);
        }
        Objects.requireNonNull(drawDate, "추첨일(drawDate)은 null일 수 없습니다.");
        Objects.requireNonNull(combination, "당첨 조합(combination)은 null일 수 없습니다.");
        if (bonusNumber < LottoCombination.MIN_NUMBER || bonusNumber > LottoCombination.MAX_NUMBER) {
            throw new IllegalArgumentException(
                    "보너스 번호는 " + LottoCombination.MIN_NUMBER + "~" + LottoCombination.MAX_NUMBER + " 범위여야 합니다: " + bonusNumber);
        }
        if (combination.contains(bonusNumber)) {
            throw new IllegalArgumentException("보너스 번호는 본번호와 중복될 수 없습니다: " + bonusNumber);
        }
        requireNonNegative(firstPrize,      "1등 당첨금(firstPrize)");
        requireNonNegative(firstWinners,    "1등 당첨자수(firstWinners)");
        requireNonNegative(totalSales,      "총 판매액(totalSales)");
        requireNonNegative(firstAccumAmount,"1등 총 당첨금(firstAccumAmount)");
        requireNonNegative(secondPrize,     "2등 당첨금(secondPrize)");
        requireNonNegative(secondWinners,   "2등 당첨자수(secondWinners)");
        if (rawJson != null && rawJson.isBlank()) {
            rawJson = null;
        }
    }

    private static void requireNonNegative(long value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + "은 음수일 수 없습니다: " + value);
        }
    }
}
