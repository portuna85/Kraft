package com.kraft.lotto.feature.winningnumber.domain;

public record WinningStore(
        int round,
        int grade,
        String name,
        String address,
        int winCount,
        String sido,
        String sigungu,
        String purchaseMethod,
        String source
) {
    public static WinningStore of(int round, int grade, String name, String address, int winCount) {
        return new WinningStore(round, grade, name, address != null ? address : "", winCount, null, null, "", "");
    }

    public static WinningStore withSource(int round, int grade, String name, String address,
                                          int winCount, String source) {
        return new WinningStore(round, grade, name, address != null ? address : "", winCount, null, null, "", source != null ? source : "");
    }
}
