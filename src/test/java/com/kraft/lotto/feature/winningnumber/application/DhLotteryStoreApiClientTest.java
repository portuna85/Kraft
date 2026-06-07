package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kraft.lotto.feature.winningnumber.domain.WinningStore;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

@DisplayName("DhLotteryStoreApiClient")
class DhLotteryStoreApiClientTest {

    DhLotteryStoreApiClient client;

    @BeforeEach
    void setUp() {
        client = new DhLotteryStoreApiClient(
                mock(RestClient.class), new ObjectMapper(),
                "https://www.dhlottery.co.kr/store.do");
    }

    @Test
    @DisplayName("정상 JSON에서 판매점 목록을 파싱한다")
    void parsesValidJson() {
        String json = """
                {"arrWinInfo":[
                  {"BPLC_NM":"거봉마트","BPLC_ADRS":"대구 서구 북비산로 310","WIN_CNT":"1"},
                  {"BPLC_NM":"교통카드충전소","BPLC_ADRS":"경기 용인시 명지로 31","WIN_CNT":"2"}
                ]}""";

        List<WinningStore> stores = client.parse(1226, 1, json);

        assertThat(stores).hasSize(2);
        assertThat(stores.get(0).name()).isEqualTo("거봉마트");
        assertThat(stores.get(0).address()).isEqualTo("대구 서구 북비산로 310");
        assertThat(stores.get(0).winCount()).isEqualTo(1);
        assertThat(stores.get(1).winCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("빈 arrWinInfo 배열이면 빈 목록을 반환한다")
    void returnsEmptyForEmptyArray() {
        assertThat(client.parse(1226, 1, "{\"arrWinInfo\":[]}")).isEmpty();
    }

    @Test
    @DisplayName("arrWinInfo 필드가 없으면 빈 목록을 반환한다")
    void returnsEmptyForMissingField() {
        assertThat(client.parse(1226, 1, "{\"other\":[]}")).isEmpty();
    }

    @Test
    @DisplayName("유효하지 않은 JSON이면 빈 목록을 반환한다")
    void returnsEmptyForInvalidJson() {
        assertThat(client.parse(1226, 1, "not-json")).isEmpty();
    }

    @Test
    @DisplayName("상점명이 없는 항목은 건너뛴다")
    void skipsEntryWithBlankName() {
        String json = """
                {"arrWinInfo":[
                  {"BPLC_NM":"","BPLC_ADRS":"주소","WIN_CNT":"1"},
                  {"BPLC_NM":"정상상점","BPLC_ADRS":"정상주소","WIN_CNT":"1"}
                ]}""";

        List<WinningStore> stores = client.parse(1226, 1, json);

        assertThat(stores).hasSize(1);
        assertThat(stores.get(0).name()).isEqualTo("정상상점");
    }

    @Test
    @DisplayName("WIN_CNT가 숫자가 아니면 1로 처리한다")
    void defaultsWinCountOnParseError() {
        String json = """
                {"arrWinInfo":[{"BPLC_NM":"상점","BPLC_ADRS":"주소","WIN_CNT":"invalid"}]}""";

        List<WinningStore> stores = client.parse(1226, 1, json);

        assertThat(stores).hasSize(1);
        assertThat(stores.get(0).winCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("fetchStores: 응답 바디가 blank이면 빈 목록을 반환한다")
    void fetchStoresReturnsEmptyForBlankBody() {
        assertThat(client.parse(1226, 1, "")).isEmpty();
        assertThat(client.parse(1226, 1, "   ")).isEmpty();
    }

    @Test
    @DisplayName("ensureWcCookie: cookieManager가 null이면 아무것도 하지 않는다")
    void ensureWcCookieSkipsWhenCookieManagerIsNull() {
        var c = new DhLotteryStoreApiClient(
                mock(RestClient.class), new ObjectMapper(),
                "https://www.dhlottery.co.kr/store.do", null, null, null, null);

        c.ensureWcCookie();

        assertThat(c.getWcCookie()).isEmpty();
    }

    @Test
    @DisplayName("ensureWcCookie: wcCookie가 없으면 생성한다")
    void ensureWcCookieCreatesCookieWhenAbsent() {
        CookieManager manager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        var c = new DhLotteryStoreApiClient(
                mock(RestClient.class), new ObjectMapper(),
                "https://www.dhlottery.co.kr/store.do", null, manager, null, "ua");

        c.ensureWcCookie();

        assertThat(c.getWcCookie()).matches("_T_\\d{5}_WC");
    }

    @Test
    @DisplayName("ensureWcCookie: 이미 있으면 덮어쓰지 않는다")
    void ensureWcCookieDoesNotOverwriteExisting() {
        CookieManager manager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        var c = new DhLotteryStoreApiClient(
                mock(RestClient.class), new ObjectMapper(),
                "https://www.dhlottery.co.kr/store.do", null, manager, null, "ua");

        c.ensureWcCookie();
        String first = c.getWcCookie();
        c.ensureWcCookie();

        assertThat(c.getWcCookie()).isEqualTo(first);
    }
}
