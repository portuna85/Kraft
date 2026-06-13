package com.kraft.winningnumber;

import com.kraft.operationlog.WinningNumberOperationLogService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WinningNumberCollectionServiceTest {

    @Test
    void collectLatestUsesNextRoundFromRepository() {
        WinningNumberRepository repository = mock(WinningNumberRepository.class);
        ExternalWinningNumberFetchClient fetchClient = mock(ExternalWinningNumberFetchClient.class);
        WinningNumberCommandService commandService = mock(WinningNumberCommandService.class);
        WinningNumberOperationLogService operationLogService = mock(WinningNumberOperationLogService.class);

        when(repository.findTopByOrderByRoundDesc()).thenReturn(Optional.of(new WinningNumber(
                1200,
                LocalDate.of(2026, 6, 13),
                3, 11, 19, 28, 34, 42,
                7,
                2_000_000_000L,
                0L, 0, 0L, 0L, null,
                OffsetDateTime.now(ZoneId.of("Asia/Seoul"))
        )));

        WinningNumberUpsertRequest fetched = new WinningNumberUpsertRequest(
                1201,
                LocalDate.of(2026, 6, 20),
                java.util.List.of(5, 12, 18, 27, 36, 44),
                9,
                2_100_000_000L,
                null, null, null, null, null
        );
        when(fetchClient.fetchRound(1201)).thenReturn(fetched);
        when(commandService.upsert(fetched)).thenReturn(new WinningNumberResponse(
                1201,
                LocalDate.of(2026, 6, 20),
                java.util.List.of(5, 12, 18, 27, 36, 44),
                9,
                2_100_000_000L,
                0L, 0, 0L, 0L
        ));

        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        WinningNumberCollectionService service = new WinningNumberCollectionService(
                repository,
                fetchClient,
                commandService,
                operationLogService,
                eventPublisher
        );
        WinningNumberResponse response = service.collectLatest();

        ArgumentCaptor<Integer> roundCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(fetchClient).fetchRound(roundCaptor.capture());

        assertThat(roundCaptor.getValue()).isEqualTo(1201);
        assertThat(response.round()).isEqualTo(1201);
    }
}
