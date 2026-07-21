package com.kraft.recommend;

import java.util.List;

/**
 * strategy/algorithmVersion/historyThroughRound: "50개 표본 중 점수가 가장 높은 조합"이라는
 * 실제 알고리즘과 그 근거가 된 이력 회차를 응답에 명시해, 클라이언트가 "당첨 확률을 높인다"가
 * 아니라 검증 가능한 휴리스틱임을 보여줄 수 있게 한다.
 */
public record RecommendNumbersResponse(
        List<List<Integer>> recommendations,
        String strategy,
        String algorithmVersion,
        int historyThroughRound
) {
}
