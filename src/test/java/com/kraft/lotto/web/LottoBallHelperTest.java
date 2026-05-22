package com.kraft.lotto.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("로또 볼 도우미 색상 그룹 테스트")
class LottoBallHelperTest {

    private final LottoBallHelper helper = new LottoBallHelper();

    @ParameterizedTest(name = "{0}번 공 → {1} 그룹")
    @CsvSource({
        "1,  1",
        "10, 1",
        "11, 2",
        "20, 2",
        "21, 3",
        "30, 3",
        "31, 4",
        "40, 4",
        "41, 5",
        "45, 5",
    })
    @DisplayName("번호 구간별 색상 그룹 반환")
    void colorGroup(int num, int expectedGroup) {
        assertThat(helper.colorGroup(num)).isEqualTo(expectedGroup);
    }
}
