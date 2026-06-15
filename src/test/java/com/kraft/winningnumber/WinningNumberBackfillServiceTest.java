package com.kraft.winningnumber;

import com.kraft.common.error.ApiException;
import java.time.LocalDate;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("WinningNumberBackfillService unit tests")
class WinningNumberBackfillServiceTest {

    @Mock
    private WinningNumberRepository repository;

    @Mock
    private ExternalWinningNumberFetchClient fetchClient;

    @Mock
    private WinningNumberCommandService commandService;

    @Mock
    private WinningNumberCollectionService collectionService;

    private WinningNumberBackfillService service;

    @BeforeEach
    void setUp() {
        service = new WinningNumberBackfillService(
                repository,
                fetchClient,
                commandService,
                collectionService,
                0,
                0,
                2
        );
    }

    @Test
    @DisplayName("empty DB backfills from round 1 until source reports no data")
    void backfillAll_emptyDb_collectsUntilRoundNotFound() {
        given(repository.findTopByOrderByRoundDesc()).willReturn(Optional.empty());
        given(fetchClient.fetchRound(1)).willReturn(request(1));
        given(fetchClient.fetchRound(2)).willReturn(request(2));
        given(fetchClient.fetchRound(3)).willReturn(request(3));
        given(fetchClient.fetchRound(4)).willThrow(sourceException("LOTTO_SOURCE_ROUND_NOT_FOUND"));
        given(commandService.upsertWithResult(request(1))).willReturn(changedResult());
        given(commandService.upsertWithResult(request(2))).willReturn(changedResult());
        given(commandService.upsertWithResult(request(3))).willReturn(changedResult());

        BackfillResult result = service.backfillAll();

        assertThat(result.startRound()).isEqualTo(1);
        assertThat(result.lastCollectedRound()).isEqualTo(3);
        assertThat(result.collectedCount()).isEqualTo(3);
        assertThat(result.updatedCount()).isEqualTo(3);
        verify(collectionService).publishBulkCollected(3);
    }

    @Test
    @DisplayName("existing latest round starts from next round and does not publish when nothing collected")
    void backfillAll_existingLatestStopsImmediatelyWithoutPublishing() {
        WinningNumber latest = mock(WinningNumber.class);
        given(latest.getRound()).willReturn(500);
        given(repository.findTopByOrderByRoundDesc()).willReturn(Optional.of(latest));
        given(fetchClient.fetchRound(501)).willThrow(sourceException("LOTTO_SOURCE_ROUND_NOT_FOUND"));

        BackfillResult result = service.backfillAll();

        assertThat(result.startRound()).isEqualTo(501);
        assertThat(result.lastCollectedRound()).isNull();
        assertThat(result.collectedCount()).isZero();
        assertThat(result.updatedCount()).isZero();
        verify(collectionService, never()).publishBulkCollected(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    @DisplayName("transient source error is retried before collecting the same round")
    void backfillAll_transientErrorRetriesSameRoundAndCollects() {
        given(repository.findTopByOrderByRoundDesc()).willReturn(Optional.empty());
        given(fetchClient.fetchRound(1))
                .willThrow(sourceException("LOTTO_SOURCE_CIRCUIT_OPEN"))
                .willReturn(request(1));
        given(fetchClient.fetchRound(2)).willThrow(sourceException("LOTTO_SOURCE_ROUND_NOT_FOUND"));
        given(commandService.upsertWithResult(request(1))).willReturn(changedResult());

        BackfillResult result = service.backfillAll();

        assertThat(result.startRound()).isEqualTo(1);
        assertThat(result.lastCollectedRound()).isEqualTo(1);
        assertThat(result.collectedCount()).isEqualTo(1);
        verify(fetchClient, times(2)).fetchRound(1);
        verify(commandService).upsertWithResult(request(1));
        verify(collectionService).publishBulkCollected(1);
    }

    @Test
    @DisplayName("transient source errors stop after max retries are exceeded")
    void backfillAll_transientErrorsStopAfterMaxRetriesExceeded() {
        RuntimeException temporary = new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "LOTTO_SOURCE_CIRCUIT_OPEN",
                "temporary source failure"
        );
        given(repository.findTopByOrderByRoundDesc()).willReturn(Optional.empty());
        given(fetchClient.fetchRound(1)).willThrow(temporary);

        BackfillResult result = service.backfillAll();

        assertThat(result.startRound()).isEqualTo(1);
        assertThat(result.lastCollectedRound()).isNull();
        assertThat(result.collectedCount()).isZero();
        assertThat(result.stopReason()).contains("1", "3", "temporary source failure");
        verify(fetchClient, times(3)).fetchRound(1);
        verify(commandService, never()).upsertWithResult(org.mockito.ArgumentMatchers.any());
        verify(collectionService, never()).publishBulkCollected(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    @DisplayName("disabled source is treated as fatal and stops immediately")
    void backfillAll_disabledSourceStopsImmediately() {
        given(repository.findTopByOrderByRoundDesc()).willReturn(Optional.empty());
        given(fetchClient.fetchRound(1)).willThrow(new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "LOTTO_SOURCE_DISABLED",
                "source disabled"
        ));

        BackfillResult result = service.backfillAll();

        assertThat(result.startRound()).isEqualTo(1);
        assertThat(result.lastCollectedRound()).isNull();
        assertThat(result.collectedCount()).isZero();
        assertThat(result.stopReason()).contains("1", "source disabled");
        verify(fetchClient).fetchRound(1);
        verify(collectionService, never()).publishBulkCollected(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    @DisplayName("invalid source payload is treated as end of data")
    void backfillAll_invalidSourcePayloadStopsAsEndOfData() {
        given(repository.findTopByOrderByRoundDesc()).willReturn(Optional.empty());
        given(fetchClient.fetchRound(1)).willThrow(sourceException("LOTTO_SOURCE_INVALID"));

        BackfillResult result = service.backfillAll();

        assertThat(result.startRound()).isEqualTo(1);
        assertThat(result.lastCollectedRound()).isNull();
        assertThat(result.collectedCount()).isZero();
        verify(fetchClient).fetchRound(1);
        verify(commandService, never()).upsertWithResult(org.mockito.ArgumentMatchers.any());
        verify(collectionService, never()).publishBulkCollected(org.mockito.ArgumentMatchers.anyInt());
    }

    private WinningNumberUpsertRequest request(int round) {
        return new WinningNumberUpsertRequest(
                round,
                LocalDate.of(2026, 1, 1).plusWeeks(round - 1L),
                List.of(1, 2, 3, 4, 5, 6),
                7,
                1_000_000_000L,
                null,
                null,
                null,
                null
        );
    }

    private WinningNumberUpsertResult changedResult() {
        return new WinningNumberUpsertResult(null, true);
    }

    private RuntimeException sourceException(String code) {
        return new ApiException(HttpStatus.BAD_GATEWAY, code, code);
    }
}
