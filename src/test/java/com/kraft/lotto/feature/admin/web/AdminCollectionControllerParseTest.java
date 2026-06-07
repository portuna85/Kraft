package com.kraft.lotto.feature.admin.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.kraft.lotto.feature.winningnumber.domain.WinningStore;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AdminCollectionController - parseStoresText")
class AdminCollectionControllerParseTest {

    @Test
    @DisplayName("정상 라인을 판매점 목록으로 파싱한다")
    void parsesNormalLines() {
        String text = "행운복권방|서울 강남구 테헤란로 1|2\n미래복권|부산 해운대구 센텀로 2|1";

        List<WinningStore> stores = AdminCollectionController.parseStoresText(1227, 1, text);

        assertThat(stores).hasSize(2);
        assertThat(stores.get(0).name()).isEqualTo("행운복권방");
        assertThat(stores.get(0).address()).isEqualTo("서울 강남구 테헤란로 1");
        assertThat(stores.get(0).winCount()).isEqualTo(2);
        assertThat(stores.get(1).winCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("빈 줄과 # 주석 줄은 건너뛴다")
    void skipsEmptyAndCommentLines() {
        String text = "행운복권방|서울|1\n#이것은 주석\n   \n미래복권|부산|1";

        List<WinningStore> stores = AdminCollectionController.parseStoresText(1227, 1, text);

        assertThat(stores).hasSize(2);
    }

    @Test
    @DisplayName("주소와 당첨건수가 없으면 기본값을 사용한다")
    void defaultsAddressAndWinCount() {
        List<WinningStore> stores = AdminCollectionController.parseStoresText(1227, 1, "행운복권방");

        assertThat(stores).hasSize(1);
        assertThat(stores.get(0).address()).isEmpty();
        assertThat(stores.get(0).winCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("이름이 빈 항목은 건너뛴다")
    void skipsEntryWithBlankName() {
        List<WinningStore> stores = AdminCollectionController.parseStoresText(1227, 1, "|주소|1");

        assertThat(stores).isEmpty();
    }

    @Test
    @DisplayName("당첨건수가 숫자가 아니면 기본값 1을 사용한다")
    void defaultsWinCountOnParseError() {
        List<WinningStore> stores = AdminCollectionController.parseStoresText(1227, 1, "행운복권방|서울|abc");

        assertThat(stores).hasSize(1);
        assertThat(stores.get(0).winCount()).isEqualTo(1);
    }
}
