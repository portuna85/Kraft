package com.kraft.winningnumber;

import com.kraft.common.error.ApiException;
import com.kraft.common.lotto.LottoNumberCodec;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("WinningNumberCommandService 단위 테스트")
class WinningNumberCommandServiceTest {

    @Mock
    private WinningNumberRepository repository;

    private WinningNumberCommandService service;

    private static final LocalDate DRAW_DATE = LocalDate.of(2024, 1, 6);
    private static final List<Integer> NUMBERS = List.of(1, 2, 3, 4, 5, 6);
    private static final int BONUS = 7;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-14T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        service = new WinningNumberCommandService(repository, new LottoNumberCodec(), clock);
    }

    private WinningNumberUpsertRequest request(List<Integer> numbers, int bonus) {
        return new WinningNumberUpsertRequest(1, DRAW_DATE, numbers, bonus,
                1_000_000_000L, null, null, null, null);
    }

    private WinningNumber buildEntity() {
        return new WinningNumber(1, DRAW_DATE, 1, 2, 3, 4, 5, 6, BONUS,
                1_000_000_000L, 0L, 0, 0L, 0L,
                java.time.OffsetDateTime.now(java.time.Clock.systemDefaultZone()));
    }

    @Test
    @DisplayName("보너스 번호가 본번호와 중복이면 BAD_REQUEST ApiException 발생")
    void upsert_bonusDuplicatesMainNumber_throwsBadRequest() {
        assertThatThrownBy(() -> service.upsert(request(NUMBERS, 1)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiEx.getCode()).isEqualTo("INVALID_BONUS_NUMBER");
                });
    }

    @Test
    @DisplayName("신규 회차 upsert 시 changed=true 반환")
    void upsertWithResult_newRound_changedTrue() {
        given(repository.findByRound(1)).willReturn(Optional.empty());
        given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

        WinningNumberUpsertResult result = service.upsertWithResult(request(NUMBERS, BONUS));

        assertThat(result.changed()).isTrue();
    }

    @Test
    @DisplayName("변경 없는 기존 회차 upsert 시 changed=false 반환")
    void upsertWithResult_existingUnchanged_changedFalse() {
        WinningNumber existing = buildEntity();
        given(repository.findByRound(1)).willReturn(Optional.of(existing));
        given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

        WinningNumberUpsertResult result = service.upsertWithResult(request(NUMBERS, BONUS));

        assertThat(result.changed()).isFalse();
    }

    @Test
    @DisplayName("1등 금액 변경 시 changed=true 반환")
    void upsertWithResult_prizeAmountChanged_changedTrue() {
        WinningNumber existing = buildEntity();
        given(repository.findByRound(1)).willReturn(Optional.of(existing));
        given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

        WinningNumberUpsertRequest changedRequest = new WinningNumberUpsertRequest(
                1, DRAW_DATE, NUMBERS, BONUS, 2_000_000_000L, null, null, null, null);

        WinningNumberUpsertResult result = service.upsertWithResult(changedRequest);

        assertThat(result.changed()).isTrue();
    }
}
