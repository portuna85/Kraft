package com.kraft.lotto.feature.statistics.application;

import java.util.HashMap;
import java.util.Map;

/**
 * 로또 6/45 이론적 조합 분포를 계산한다.
 * <p>
 * - 홀짝 분포: C(23, oddCount) * C(22, 6-oddCount) / C(45, 6)
 * - 합산 분포: DP로 {1..45}에서 6개를 고를 때 합이 s가 되는 조합 수 / C(45, 6)
 * <p>
 * 모든 값은 미리 계산되어 상수로 보관된다.
 */
final class LottoTheoreticalDistribution {

    static final int TOTAL = 6;
    static final int MAX_NUMBER = 45;
    static final long TOTAL_COMBINATIONS = 8_145_060L; // C(45,6)

    // 홀수 개수(0~6)별 이론 비율 (%)
    private static final double[] ODD_EVEN_PERCENT = computeOddEvenPercents();

    // 합산값(21~255)별 조합 수
    private static final long[] SUM_COUNTS = computeSumCounts();

    private LottoTheoreticalDistribution() {
    }

    /** oddCount(0~6)에 해당하는 이론 출현 비율(%)을 반환한다. */
    static double oddEvenPercent(int oddCount) {
        if (oddCount < 0 || oddCount > TOTAL) {
            return 0.0;
        }
        return ODD_EVEN_PERCENT[oddCount];
    }

    /**
     * 10 단위 버킷별 이론 비율(%) 맵을 반환한다.
     * 키: 버킷 시작값(20, 30, ... 250), 값: 이론 비율(%)
     */
    static Map<Integer, Double> sumBucketPercents(int bucketSize) {
        Map<Integer, Double> result = new HashMap<>();
        for (int sum = 21; sum <= 255; sum++) {
            int bucket = (sum / bucketSize) * bucketSize;
            long count = SUM_COUNTS[sum];
            result.merge(bucket, count * 100.0 / TOTAL_COMBINATIONS, Double::sum);
        }
        return result;
    }

    // --- 계산 ---

    private static double[] computeOddEvenPercents() {
        // 1~45 중 홀수: 1,3,5,...,45 → 23개 / 짝수: 2,4,...,44 → 22개
        int odds = 23;
        int evens = 22;
        double[] result = new double[TOTAL + 1];
        for (int k = 0; k <= TOTAL; k++) {
            long ways = combination(odds, k) * combination(evens, TOTAL - k);
            result[k] = ways * 100.0 / TOTAL_COMBINATIONS;
        }
        return result;
    }

    /**
     * DP로 {1..45}에서 6개를 선택할 때 합이 각 s인 조합 수를 계산한다.
     * dp[pick][sum] = 1..n 범위에서 pick개를 골라 합이 sum인 경우의 수
     */
    private static long[] computeSumCounts() {
        int maxSum = 255;
        // dp[k][s]: 현재까지 본 숫자들 중 k개를 골라 합이 s인 경우의 수
        long[][] dp = new long[TOTAL + 1][maxSum + 1];
        dp[0][0] = 1L;

        for (int n = 1; n <= MAX_NUMBER; n++) {
            // 역순으로 순회해야 같은 숫자를 두 번 사용하지 않음
            for (int k = Math.min(n, TOTAL); k >= 1; k--) {
                for (int s = maxSum; s >= n; s--) {
                    dp[k][s] += dp[k - 1][s - n];
                }
            }
        }

        long[] counts = new long[maxSum + 1];
        System.arraycopy(dp[TOTAL], 0, counts, 0, maxSum + 1);
        return counts;
    }

    private static long combination(int n, int r) {
        if (r < 0 || r > n) {
            return 0L;
        }
        if (r == 0 || r == n) {
            return 1L;
        }
        r = Math.min(r, n - r);
        long result = 1L;
        for (int i = 0; i < r; i++) {
            result = result * (n - i) / (i + 1);
        }
        return result;
    }
}
