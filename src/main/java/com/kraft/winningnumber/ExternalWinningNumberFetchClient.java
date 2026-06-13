package com.kraft.winningnumber;

public interface ExternalWinningNumberFetchClient {

    WinningNumberUpsertRequest fetchRound(int round);
}
