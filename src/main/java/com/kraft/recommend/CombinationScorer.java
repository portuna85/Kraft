package com.kraft.recommend;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 로또 조합의 "비인기도" 점수를 계산한다.
 *
 * 점수가 높을수록 다른 구매자들이 동일 조합을 선택할 확률이 낮다.
 * 1등 당첨 시 공동 당첨자가 적어 개인 수령액이 높아진다.
 *
 * 반영된 편향:
 * - 생일 편향: 1~31번은 생년월일 선택으로 과다 선택됨 → 32~45 선호
 * - 한 자리 편향: 1~9번은 "행운 번호"로 특히 인기
 * - 라운드 번호 편향: 5·10·15·20·25·30·35·40·45, 7·14·21·28·35·42 인기
 * - 낮은 합계 편향: 생일 번호 선택으로 합계가 낮은 조합이 혼잡
 * - 연속 번호: 패턴으로 선택되는 경향 있음
 */
@Component
public class CombinationScorer {

    public int score(List<Integer> sorted) {
        int score = 0;
        int sum = 0;

        for (int n : sorted) {
            sum += n;

            if (n > 31) { score += 3; }   // 생일 편향 역방향: 32~45 선호
            if (n <= 9) { score -= 4; }   // 한 자리 숫자는 특히 인기
            if (n % 5 == 0) { score -= 2; } // 라운드 번호(5 배수) 인기
            if (n % 7 == 0) { score -= 1; } // 7 및 배수 "행운"으로 인기
        }

        // 합계 구간: 낮은 합계는 혼잡, 130+ 구간은 상대적으로 여유
        if (sum < 100) { score -= 5; }
        else if (sum >= 130 && sum <= 220) { score += 4; }

        // 연속 쌍: 패턴 선택으로 가산 페널티
        for (int i = 0; i < sorted.size() - 1; i++) {
            if (sorted.get(i + 1) - sorted.get(i) == 1) { score -= 1; }
        }

        return score;
    }
}
