package com.kraft.common.lotto;

public final class BallClassification {

    private BallClassification() {}

    public static boolean isHigh(int ball) {
        return ball >= 23;
    }

    public static boolean isOdd(int ball) {
        return ball % 2 != 0;
    }
}
