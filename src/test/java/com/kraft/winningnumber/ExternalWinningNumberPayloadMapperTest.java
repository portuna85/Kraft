package com.kraft.winningnumber;

import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalWinningNumberPayloadMapperTest {

    private final ExternalWinningNumberPayloadMapper mapper = new ExternalWinningNumberPayloadMapper();

    @Test
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
}
