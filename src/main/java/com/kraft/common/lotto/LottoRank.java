package com.kraft.common.lotto;

public final class LottoRank {

    private LottoRank() {}

    public static String of(int matchedCount, boolean bonusMatched) {
        return switch (matchedCount) {
            case 6 -> "1등";
            case 5 -> bonusMatched ? "2등" : "3등";
            case 4 -> "4등";
            case 3 -> "5등";
            default -> "낙첨";
        };
    }
}
