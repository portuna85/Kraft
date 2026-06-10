package com.kraft.lotto.feature.news.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("뉴스 관련성 정책")
class NewsRelevancePolicyTest {

    private final NewsRelevancePolicy policy = new NewsRelevancePolicy();

    @Test
    @DisplayName("동행복권 키워드가 있으면 승인")
    void approveOnDhLottery() {
        NewsDecision d = policy.decide("동행복권 1227회 당첨번호 발표", null, null, null);
        assertThat(d.type()).isEqualTo(NewsDecision.Type.APPROVE);
        assertThat(d.score()).isGreaterThanOrEqualTo(4);
    }

    @Test
    @DisplayName("당첨번호 + 추첨 키워드 조합이면 승인")
    void approveOnDrawResult() {
        NewsDecision d = policy.decide("이번 회차 당첨번호와 추첨 결과", null, null, null);
        assertThat(d.type()).isEqualTo(NewsDecision.Type.APPROVE);
    }

    @Test
    @DisplayName("청약·분양 키워드가 있으면 거절")
    void rejectOnRealEstate() {
        NewsDecision d = policy.decide("로또 청약 아파트 분양 정보", null, null, null);
        assertThat(d.type()).isEqualTo(NewsDecision.Type.REJECT);
        assertThat(d.score()).isLessThan(0);
    }

    @Test
    @DisplayName("부동산·전세 키워드만 있으면 거절")
    void rejectOnRealEstateOnly() {
        NewsDecision d = policy.decide("강남 전세 부동산 급등", null, null, null);
        assertThat(d.type()).isEqualTo(NewsDecision.Type.REJECT);
    }

    @Test
    @DisplayName("설명에 판매점 신호가 있으면 승인 가능")
    void approveOnDescriptionSignal() {
        NewsDecision d = policy.decide("로또 관련 뉴스", "이번 회 1등 당첨 판매점 공개", null, null);
        assertThat(d.type()).isEqualTo(NewsDecision.Type.APPROVE);
    }

    @Test
    @DisplayName("출처에 동행복권 신호가 있으면 승인 가능")
    void approveOnSourceSignal() {
        NewsDecision d = policy.decide("공지", null, "동행복권 공식 안내", null);
        assertThat(d.type()).isEqualTo(NewsDecision.Type.APPROVE);
    }

    @Test
    @DisplayName("점수=0이면 검토")
    void reviewOnZeroScore() {
        // '로또'만 언급하고 신호 없음
        NewsDecision d = policy.decide("로또 관련 일반 기사", null, null, null);
        assertThat(d.type()).isEqualTo(NewsDecision.Type.REVIEW);
        assertThat(d.score()).isEqualTo(0);
    }

    @Test
    @DisplayName("점수=3이면 검토 (임계값=4 미만)")
    void reviewOnScoreBeforeThreshold() {
        // 추첨(+3)만 있으면 score=3 → REVIEW
        NewsDecision d = policy.decide("이번 주 추첨 방송 안내", null, null, null);
        assertThat(d.type()).isEqualTo(NewsDecision.Type.REVIEW);
        assertThat(d.score()).isEqualTo(3);
    }

    @Test
    @DisplayName("허용와 차단 신호가 섞이면 합산 결과로 판단")
    void mixedSignals() {
        // 동행복권(+5) + 청약(-5) = 0 → REVIEW
        NewsDecision d = policy.decide("동행복권 청약 이벤트", null, null, null);
        assertThat(d.type()).isEqualTo(NewsDecision.Type.REVIEW);
        assertThat(d.score()).isEqualTo(0);
    }

    @Test
    @DisplayName("거절 결정의 거절 사유은 점수 포함")
    void rejectReasonIncludesScore() {
        NewsDecision d = policy.decide("전세 부동산 청약 로또 분양", null, null, null);
        assertThat(d.type()).isEqualTo(NewsDecision.Type.REJECT);
        assertThat(d.rejectReason()).startsWith("score:");
        assertThat(d.rejectReason()).contains(String.valueOf(d.score()));
    }

    @Test
    @DisplayName("승인 결정의 거절 사유은 널")
    void noRejectReasonWhenApproved() {
        NewsDecision d = policy.decide("동행복권 당첨번호 발표", null, null, null);
        assertThat(d.rejectReason()).isNull();
    }

    @Test
    @DisplayName("모든 입력이 널이어도 예외 없이 검토 반환")
    void nullInputsReturnReview() {
        NewsDecision d = policy.decide(null, null, null, null);
        assertThat(d.type()).isEqualTo(NewsDecision.Type.REVIEW);
        assertThat(d.score()).isEqualTo(0);
    }
}
