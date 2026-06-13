package com.kraft.winningnumber;

import java.time.LocalDate;
import java.util.List;

public record WinningNumberResponse(
        int round,
        LocalDate drawDate,
        List<Integer> numbers,
        int bonusNumber,
        long firstPrizeAmount,
        long secondPrize,
        int secondWinners,
        long totalSales,
        long firstAccumAmount
) {
    static WinningNumberResponse from(WinningNumber winningNumber) {
        return new WinningNumberResponse(
                winningNumber.getRound(),
                winningNumber.getDrawDate(),
                List.of(
                        winningNumber.getN1(),
                        winningNumber.getN2(),
                        winningNumber.getN3(),
                        winningNumber.getN4(),
                        winningNumber.getN5(),
                        winningNumber.getN6()
                ),
                winningNumber.getBonusNumber(),
                winningNumber.getFirstPrizeAmount(),
                winningNumber.getSecondPrize() != null ? winningNumber.getSecondPrize() : 0L,
                winningNumber.getSecondWinners() != null ? winningNumber.getSecondWinners() : 0,
                winningNumber.getTotalSales() != null ? winningNumber.getTotalSales() : 0L,
                winningNumber.getFirstAccumAmount() != null ? winningNumber.getFirstAccumAmount() : 0L
        );
    }
}
