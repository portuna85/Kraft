package com.kraft.winningnumber;

import com.kraft.common.error.ApiException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("외부 로또 수집 클라이언트 — 회차 일치 검증 테스트")
class HttpExternalWinningNumberFetchClientTest {

    @Test
    @DisplayName("응답 회차가 요청 회차와 다르면 LOTTO_SOURCE_ROUND_MISMATCH를 던진다")
    void requireRoundMatch_mismatchedRound_throwsRoundMismatch() {
        WinningNumberUpsertRequest response = request(1202);

        assertThatThrownBy(() -> HttpExternalWinningNumberFetchClient.requireRoundMatch(1201, response))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(apiEx.getCode()).isEqualTo("LOTTO_SOURCE_ROUND_MISMATCH");
                });
    }

    @Test
    @DisplayName("응답 회차가 요청 회차와 같으면 예외 없이 통과한다")
    void requireRoundMatch_matchingRound_doesNotThrow() {
        WinningNumberUpsertRequest response = request(1201);

        assertThatCode(() -> HttpExternalWinningNumberFetchClient.requireRoundMatch(1201, response))
                .doesNotThrowAnyException();
    }

    private WinningNumberUpsertRequest request(int round) {
        return new WinningNumberUpsertRequest(
                round, LocalDate.of(2026, 6, 20), List.of(5, 12, 18, 27, 36, 44), 9,
                2_100_000_000L, null, null, null, null
        );
    }
}
