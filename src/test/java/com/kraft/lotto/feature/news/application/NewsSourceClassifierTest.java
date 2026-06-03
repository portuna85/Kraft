package com.kraft.lotto.feature.news.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.kraft.lotto.feature.news.domain.NewsSourceTier;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("뉴스 출처 신뢰도 분류기")
class NewsSourceClassifierTest {

    private final NewsSourceClassifier classifier = new NewsSourceClassifier(
            List.of("동행복권", "기획재정부"),
            List.of("연합뉴스", "뉴시스", "조선일보")
    );

    @Test
    @DisplayName("공식 출처는 OFFICIAL로 분류된다")
    void officialSourceIsClassifiedAsOfficial() {
        assertThat(classifier.classify("동행복권")).isEqualTo(NewsSourceTier.OFFICIAL);
        assertThat(classifier.classify("기획재정부")).isEqualTo(NewsSourceTier.OFFICIAL);
    }

    @Test
    @DisplayName("언론 출처는 PRESS로 분류된다")
    void pressSourceIsClassifiedAsPress() {
        assertThat(classifier.classify("연합뉴스")).isEqualTo(NewsSourceTier.PRESS);
        assertThat(classifier.classify("조선일보")).isEqualTo(NewsSourceTier.PRESS);
    }

    @Test
    @DisplayName("미등록 출처는 GENERAL로 분류된다")
    void unknownSourceIsClassifiedAsGeneral() {
        assertThat(classifier.classify("어느 블로그")).isEqualTo(NewsSourceTier.GENERAL);
        assertThat(classifier.classify("네이버 카페")).isEqualTo(NewsSourceTier.GENERAL);
    }

    @Test
    @DisplayName("null 또는 빈 출처는 GENERAL로 분류된다")
    void nullOrBlankSourceIsGeneral() {
        assertThat(classifier.classify(null)).isEqualTo(NewsSourceTier.GENERAL);
        assertThat(classifier.classify("")).isEqualTo(NewsSourceTier.GENERAL);
        assertThat(classifier.classify("   ")).isEqualTo(NewsSourceTier.GENERAL);
    }

    @Test
    @DisplayName("공식 출처가 부분 포함되어도 OFFICIAL로 분류된다")
    void partialMatchClassifiesAsOfficial() {
        assertThat(classifier.classify("동행복권 공식")).isEqualTo(NewsSourceTier.OFFICIAL);
    }
}
