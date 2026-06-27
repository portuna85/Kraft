package com.kraft.common.lotto;

public final class SumBuckets {

    private SumBuckets() {}

    public static String bucketOf(int sum) {
        if (sum < 66) {
            return "21-65";
        }
        if (sum < 111) {
            return "66-110";
        }
        if (sum < 156) {
            return "111-155";
        }
        if (sum < 201) {
            return "156-200";
        }
        return "201-255";
    }
}
