package com.kraft.winningnumber;

import org.junit.jupiter.api.DisplayName;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("외부 당첨 번호 페이로드 매퍼 테스트")
class ExternalWinningNumberPayloadMapperTest {

    private final ExternalWinningNumberPayloadMapper mapper = new ExternalWinningNumberPayloadMapper();

    @Test
    @DisplayName("공식 스타일 페이로드가 올바르게 매핑되는지 확인")
    void mapsOfficialStylePayload() {
        WinningNumberUpsertRequest request = mapper.toRequest(Map.ofEntries(
                Map.entry("returnValue", "success"),
                Map.entry("drwNo", 1201),
                Map.entry("drwNoDate", "2026-06-20"),
                Map.entry("drwtNo1", 5),
                Map.entry("drwtNo2", 12),
                Map.entry("drwtNo3", 18),
                Map.entry("drwtNo4", 27),
                Map.entry("drwtNo5", 36),
                Map.entry("drwtNo6", 44),
                Map.entry("bnusNo", 9),
                Map.entry("firstWinamnt", 2100000000L)
        ));

        assertThat(request.round()).isEqualTo(1201);
        assertThat(request.drawDate().toString()).isEqualTo("2026-06-20");
        assertThat(request.numbers()).containsExactly(5, 12, 18, 27, 36, 44);
        assertThat(request.bonusNumber()).isEqualTo(9);
        assertThat(request.firstPrizeAmount()).isEqualTo(2100000000L);
    }

    @Test
    @DisplayName("새 lt645 API 스타일 페이로드(YYYYMMDD 날짜, tm1-6WnNo 필드)가 올바르게 매핑되는지 확인")
    void mapsNewLt645StylePayload() {
        WinningNumberUpsertRequest request = mapper.toRequest(Map.ofEntries(
                Map.entry("ltEpsd", 1227),
                Map.entry("ltRflYmd", "20260606"),
                Map.entry("tm1WnNo", 3),
                Map.entry("tm2WnNo", 11),
                Map.entry("tm3WnNo", 19),
                Map.entry("tm4WnNo", 28),
                Map.entry("tm5WnNo", 35),
                Map.entry("tm6WnNo", 42),
                Map.entry("bnsWnNo", 7),
                Map.entry("rnk1WnAmt", 1500000000L),
                Map.entry("rnk1SumWnAmt", 3000000000L),
                Map.entry("rnk2WnAmt", 50000000L),
                Map.entry("rnk2WnNope", 60),
                Map.entry("rlvtEpsdSumNtslAmt", 55000000000L)
        ));

        assertThat(request.round()).isEqualTo(1227);
        assertThat(request.drawDate().toString()).isEqualTo("2026-06-06");
        assertThat(request.numbers()).containsExactly(3, 11, 19, 28, 35, 42);
        assertThat(request.bonusNumber()).isEqualTo(7);
        assertThat(request.firstPrizeAmount()).isEqualTo(1500000000L);
        assertThat(request.firstAccumAmount()).isEqualTo(3000000000L);
        assertThat(request.secondPrize()).isEqualTo(50000000L);
        assertThat(request.secondWinners()).isEqualTo(60);
        assertThat(request.totalSales()).isEqualTo(55000000000L);
    }
}
