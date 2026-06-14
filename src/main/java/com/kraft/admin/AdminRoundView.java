package com.kraft.admin;

import com.kraft.winningnumber.WinningNumberResponse;
import java.time.LocalDate;

public record AdminRoundView(
        int round,
        LocalDate drawDate,
        int num1, int num2, int num3, int num4, int num5, int num6,
        int bonusNumber,
        long firstPrizeAmount
) {
    public static AdminRoundView from(WinningNumberResponse r) {
        var n = r.numbers();
        return new AdminRoundView(
                r.round(), r.drawDate(),
                n.get(0), n.get(1), n.get(2), n.get(3), n.get(4), n.get(5),
                r.bonusNumber(), r.firstPrizeAmount()
        );
    }
}
