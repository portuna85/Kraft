package com.kraft.lotto.web;

final class LottoPrizeTaxCalculator {

    private static final double TAX_HIGH = 0.33;
    private static final double TAX_MID  = 0.22;
    private static final long   THRESHOLD_HIGH = 300_000_000L;
    private static final long   THRESHOLD_MID  =   2_000_000L;

    private LottoPrizeTaxCalculator() {}

    static long afterTax(long prize) {
        if (prize <= 0) {
            return 0;
        }
        if (prize > THRESHOLD_HIGH) {
            return (long) (prize * (1.0 - TAX_HIGH));
        }
        if (prize > THRESHOLD_MID) {
            return (long) (prize * (1.0 - TAX_MID));
        }
        return prize;
    }

    static String taxRate(long prize) {
        if (prize > THRESHOLD_HIGH) {
            return "33%";
        }
        if (prize > THRESHOLD_MID) {
            return "22%";
        }
        return "-";
    }
}
