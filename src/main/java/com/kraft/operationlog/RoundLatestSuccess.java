package com.kraft.operationlog;

import java.time.OffsetDateTime;

/**
 * 회차별 가장 최근 SUCCESS 로그 시각 — 인시던트 해결 여부를 라운드 단위로 일괄 조회할 때 쓰는 투영.
 */
public record RoundLatestSuccess(Integer round, OffsetDateTime latestSuccessAt) {
}
