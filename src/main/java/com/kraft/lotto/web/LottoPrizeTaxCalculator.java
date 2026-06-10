package com.kraft.lotto.web;

final class LottoPrizeTaxCalculator {

    private static final long THRESHOLD_HIGH = 300_000_000L;
    private static final long THRESHOLD_MID  =   2_000_000L;

    private LottoPrizeTaxCalculator() {}

    static long afterTax(long prize) {
        if (prize <= 0) {
            return 0;
        }
        if (prize <= THRESHOLD_MID) {
            return prize;
        }
        if (prize <= THRESHOLD_HIGH) {
            return prize - prize * 22 / 100;
        }
        long taxOnFirst  = THRESHOLD_HIGH * 22 / 100;          // 3억까지 22%
        long taxOnExcess = (prize - THRESHOLD_HIGH) * 33 / 100; // 초과분만 33%
        return prize - taxOnFirst - taxOnExcess;
    }

    static String taxRate(long prize) {
        if (prize > THRESHOLD_HIGH) {
            return "최대 33%";
        }
        if (prize > THRESHOLD_MID) {
            return "22%";
        }
        return "-";
    }
}
