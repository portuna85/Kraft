package com.kraft.common.lotto;

import java.util.Set;

public final class SumBuckets {

    private SumBuckets() {}

    // bucketOf()가 만들 수 있는 전체 구간 키 집합 — 통계 summary 완전성 검증(P1-07)이
    // "일부 구간만 누락된" 부분 손상을 감지하려면 이 목록과 실제 저장된 키 집합을 비교해야
    // 하므로, bucketOf()와 별개로 하드코딩하지 않도록 단일 소스로 둔다.
    public static final Set<String> ALL_KEYS = Set.of("21-65", "66-110", "111-155", "156-200", "201-255");

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
