package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.kraft.lotto.feature.winningnumber.application.KoreanAddressParser.ParsedAddress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("KoreanAddressParser")
class KoreanAddressParserTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("null·빈 문자열·공백은 empty를 반환한다")
    void returnsEmptyForBlankInput(String address) {
        ParsedAddress result = KoreanAddressParser.parse(address);
        assertThat(result.sido()).isNull();
        assertThat(result.sigungu()).isNull();
    }

    @Test
    @DisplayName("서울특별시 → sido=서울, sigungu=강남구")
    void parsesSeoul() {
        ParsedAddress result = KoreanAddressParser.parse("서울특별시 강남구 테헤란로 123");
        assertThat(result.sido()).isEqualTo("서울");
        assertThat(result.sigungu()).isEqualTo("강남구");
    }

    @Test
    @DisplayName("경기도 + 시+구 조합 → sigungu=수원시 팔달구")
    void parsesGyeonggiSiGu() {
        ParsedAddress result = KoreanAddressParser.parse("경기도 수원시 팔달구 매산로 1");
        assertThat(result.sido()).isEqualTo("경기");
        assertThat(result.sigungu()).isEqualTo("수원시 팔달구");
    }

    @Test
    @DisplayName("충청북도 + 시+구 조합 → sigungu=청주시 흥덕구")
    void parsesChungbukSiGu() {
        ParsedAddress result = KoreanAddressParser.parse("충청북도 청주시 흥덕구 직지대로 1");
        assertThat(result.sido()).isEqualTo("충북");
        assertThat(result.sigungu()).isEqualTo("청주시 흥덕구");
    }

    @Test
    @DisplayName("부산광역시 → sido=부산, sigungu=해운대구")
    void parsesBusan() {
        ParsedAddress result = KoreanAddressParser.parse("부산광역시 해운대구 센텀중앙로 79");
        assertThat(result.sido()).isEqualTo("부산");
        assertThat(result.sigungu()).isEqualTo("해운대구");
    }

    @Test
    @DisplayName("세종특별자치시 → sido=세종, sigungu=한누리대로(2번째 토큰)")
    void parsesSeJong() {
        ParsedAddress result = KoreanAddressParser.parse("세종특별자치시 한누리대로 2130");
        assertThat(result.sido()).isEqualTo("세종");
        assertThat(result.sigungu()).isEqualTo("한누리대로");
    }

    @Test
    @DisplayName("강원특별자치도 → sido=강원")
    void parsesGangwon() {
        ParsedAddress result = KoreanAddressParser.parse("강원특별자치도 춘천시 중앙로 1");
        assertThat(result.sido()).isEqualTo("강원");
        assertThat(result.sigungu()).isEqualTo("춘천시");
    }

    @Test
    @DisplayName("정규화 테이블 미등록 행정구역은 원문 그대로 저장한다")
    void preservesUnknownSido() {
        ParsedAddress result = KoreanAddressParser.parse("신행정구역 신시군 신동");
        assertThat(result.sido()).isEqualTo("신행정구역");
        assertThat(result.sigungu()).isEqualTo("신시군");
    }

    @Test
    @DisplayName("토큰이 1개뿐이면 sigungu는 null")
    void singleTokenHasNoSimgungu() {
        ParsedAddress result = KoreanAddressParser.parse("서울특별시");
        assertThat(result.sido()).isEqualTo("서울");
        assertThat(result.sigungu()).isNull();
    }
}
