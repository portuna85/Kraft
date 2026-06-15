package com.kraft.winningnumber;

/**
 * 전체 회차 수집(백필) 결과 요약.
 *
 * @param startRound         수집을 시작한 회차 (마지막 저장 회차 + 1, 빈 DB면 1)
 * @param lastCollectedRound 마지막으로 성공 수집한 회차 (한 건도 없으면 null)
 * @param collectedCount     이번 실행에서 upsert한 회차 수
 * @param updatedCount       그중 실제로 값이 신규/변경된 회차 수
 * @param stopReason         수집이 종료된 사유 (최신 도달 / 실패 / 안전 상한 등)
 */
public record BackfillResult(
        int startRound,
        Integer lastCollectedRound,
        int collectedCount,
        int updatedCount,
        String stopReason
) {
}
