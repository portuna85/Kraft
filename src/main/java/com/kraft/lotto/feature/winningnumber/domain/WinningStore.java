package com.kraft.lotto.feature.winningnumber.domain;

public record WinningStore(
        int round,
        int grade,
        String name,
        String address,
        int winCount,
        String sido,
        String sigungu,
        String purchaseMethod
) {
    public static WinningStore of(int round, int grade, String name, String address, int winCount) {
        return new WinningStore(round, grade, name, address, winCount, null, null, null);
    }
}
