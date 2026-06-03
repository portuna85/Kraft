package com.kraft.lotto.feature.recommend.domain;

import com.kraft.lotto.feature.winningnumber.domain.LottoCombination;

/**
 * 동일 십의 자리(decade) 버킷에 5개 이상 번호가 몰린 조합을 제외한다.
 * 버킷: 1~9, 10~19, 20~29, 30~39, 40~45.
 * <p>
 * 이 버킷 정의는 "공식 공 색상" 기준(LottoBallHelper: 1~10, 11~20, …)과 의도적으로 다르다.
 * 통계적 편중 판단에는 십의 자리(tens-digit) 기준이 사용되고, 볼 색상은 KRX 공식 배색을 따른다.
 */
public class SingleDecadeRule implements ExclusionRule {

    public static final int DEFAULT_DECADE_THRESHOLD = 5;
    private static final int BUCKET_COUNT = 5;

    private final int decadeThreshold;

    public SingleDecadeRule() {
        this(DEFAULT_DECADE_THRESHOLD);
    }

    public SingleDecadeRule(int decadeThreshold) {
        this.decadeThreshold = decadeThreshold;
    }

    @Override
    public boolean shouldExclude(LottoCombination combination) {
        int[] buckets = new int[BUCKET_COUNT];
        for (Integer n : combination.numbers()) {
            buckets[bucketIndex(n)]++;
        }
        for (int count : buckets) {
            if (count >= decadeThreshold) {
                return true;
            }
        }
        return false;
    }

    private static int bucketIndex(int n) {
        if (n <= 9) {
            return 0;
        }
        if (n <= 19) {
            return 1;
        }
        if (n <= 29) {
            return 2;
        }
        if (n <= 39) {
            return 3;
        }
        return 4; // 40~45
    }

    @Override
    public String reason() {
        return "동일 십의 자리 그룹에 5개 이상 번호가 몰린 조합은 제외합니다.";
    }

    @Override
    public String label() {
        return "특정 번호대 과밀 없음";
    }
}
