package com.kraft.lotto.feature.winningnumber.domain;

public record WinningStore(
        int round,
        int grade,
        String name,
        String address,
        int winCount
) {}
