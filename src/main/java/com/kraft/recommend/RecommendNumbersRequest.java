package com.kraft.recommend;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;

public record RecommendNumbersRequest(
        @Min(1) @Max(10) Integer count,
        List<Integer> excludedNumbers,
        // 필드명이 "당첨금 최대화"처럼 읽혀 전체 조합을 최적화한다는 오해를 줄 수 있어
        // 의도(공동 당첨 위험 감소)를 드러내는 이름으로 바꿨다. 기존 클라이언트가 보내는
        // maximizePrize도 계속 받아들인다(@JsonAlias) — 필드 삭제가 아니라 이름만 바뀐
        // 전환이므로 별도 응답 스키마 변화 없이 구버전 요청도 그대로 동작한다.
        @JsonAlias("maximizePrize") Boolean reduceSharedWinnerRisk
) {
}
