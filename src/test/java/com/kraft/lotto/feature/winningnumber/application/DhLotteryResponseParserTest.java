package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("동행복권 응답 파서")
class DhLotteryResponseParserTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-20T12:00:00Z"), ZoneOffset.UTC);

    private final DhLotteryResponseParser parser = new DhLotteryResponseParser(new ObjectMapper(), CLOCK);

    @Test
    @DisplayName("성공적인 JSON 응답을 WinningNumber 객체로 파싱한다")
    void parsesSuccessResponse() {
        var result = parser.parse(1200, successBody(1200));

        assertThat(result).hasValueSatisfying(winningNumber -> {
            assertThat(winningNumber.round()).isEqualTo(1200);
            assertThat(winningNumber.drawDate()).isEqualTo(LocalDate.of(2026, 5, 16));
            assertThat(winningNumber.combination().numbers()).containsExactly(1, 2, 3, 4, 5, 6);
            assertThat(winningNumber.bonusNumber()).isEqualTo(7);
            assertThat(winningNumber.firstPrize()).isEqualTo(2_000_000_000L);
            assertThat(winningNumber.firstWinners()).isEqualTo(10);
            assertThat(winningNumber.totalSales()).isEqualTo(90_000_000_000L);
            assertThat(winningNumber.firstAccumAmount()).isEqualTo(20_000_000_000L);
            assertThat(winningNumber.rawJson()).isEqualTo(successBody(1200));
            assertThat(winningNumber.fetchedAt()).isEqualTo(LocalDateTime.ofInstant(CLOCK.instant(), CLOCK.getZone()));
        });
    }

    @Test
    @DisplayName("returnValue가 fail인 경우 빈 결과를 반환한다")
    void returnsEmptyForFailReturnValue() {
        var result = parser.parse(1201, "{\"returnValue\":\"fail\"}");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("JSON 파싱 전 HTML 응답을 거절한다")
    void rejectsHtmlResponse() {
        assertThatExceptionOfType(LottoApiClientException.class)
                .isThrownBy(() -> parser.parse(1202, "<html></html>"))
                .withMessageContaining("not JSON");
    }

    @Test
    @DisplayName("회차가 일치하지 않는 응답을 거절한다")
    void rejectsRoundMismatch() {
        assertThatExceptionOfType(LottoApiClientException.class)
                .isThrownBy(() -> parser.parse(1203, successBody(1202)))
                .withMessageContaining("round mismatch");
    }

    @Test
    @DisplayName("필수 필드가 누락된 응답을 거절한다")
    void rejectsMissingRequiredField() {
        String body = """
                {
                  "returnValue": "success",
                  "drwNo": 1204,
                  "drwNoDate": "2026-05-16"
                }
                """;

        assertThatExceptionOfType(LottoApiClientException.class)
                .isThrownBy(() -> parser.parse(1204, body))
                .withMessageContaining("field missing")
                .withMessageContaining("drwtNo1");
    }

    private static String successBody(int round) {
        return """
                {%n\
                  "returnValue": "success",%n\
                  "drwNo": %d,%n\
                  "drwNoDate": "2026-05-16",%n\
                  "drwtNo1": 1,%n\
                  "drwtNo2": 2,%n\
                  "drwtNo3": 3,%n\
                  "drwtNo4": 4,%n\
                  "drwtNo5": 5,%n\
                  "drwtNo6": 6,%n\
                  "bnusNo": 7,%n\
                  "firstWinamnt": 2000000000,%n\
                  "firstPrzwnerCo": 10,%n\
                  "totSellamnt": 90000000000,%n\
                  "firstAccumamnt": 20000000000%n\
                }%n\
                """.formatted(round);
    }
}
