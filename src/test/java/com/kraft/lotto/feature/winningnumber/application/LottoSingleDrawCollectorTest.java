package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static com.kraft.lotto.support.fixtures.LottoTestFixtures.winningNumber;

import com.kraft.lotto.feature.winningnumber.domain.WinningNumber;
import com.kraft.lotto.feature.winningnumber.infrastructure.LottoFetchLogEntity;
import com.kraft.lotto.feature.winningnumber.infrastructure.LottoFetchLogRepository;
import com.kraft.lotto.feature.winningnumber.infrastructure.LottoFetchStatus;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("단일 회차 로또 수집기")
class LottoSingleDrawCollectorTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-20T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    LottoApiClient lottoApiClient;

    @Mock
    WinningNumberRepository winningNumberRepository;

    @Mock
    WinningNumberPersister persister;

    @Mock
    LottoFetchLogRepository fetchLogRepository;

    @Test
    @DisplayName("새로고침 옵션이 꺼져 있으면 기존 회차 수집을 건너뛴다")
    void skipsExistingRoundWithoutRefresh() {
        LottoSingleDrawCollector collector = collector();
        when(winningNumberRepository.existsByRound(1200)).thenReturn(true);
        when(winningNumberRepository.findMaxRound()).thenReturn(Optional.of(1200));

        var response = collector.collectOne(1200, false);

        assertThat(response.skipped()).isEqualTo(1);
        assertThat(response.latestRound()).isEqualTo(1200);
        verifyNoInteractions(lottoApiClient, persister);
        assertSavedLog(LottoFetchStatus.SKIPPED, 1200, null, null);
    }

    @Test
    @DisplayName("batch skip 시 logSkip=false이면 skip 로그를 저장하지 않는다")
    void batchSkipDoesNotSaveLog() {
        LottoSingleDrawCollector collector = collector();
        when(winningNumberRepository.existsByRound(1200)).thenReturn(true);

        var response = collector.collectOne(1200, false, 1200, false);

        assertThat(response.skipped()).isEqualTo(1);
        verify(fetchLogRepository, never()).save(any());
        verifyNoInteractions(lottoApiClient, persister);
    }

    @Test
    @DisplayName("수집된 당첨 번호를 저장하고 성공 로그에는 원본 JSON을 생략한다")
    void storesFetchedWinningNumber() {
        LottoSingleDrawCollector collector = collector();
        WinningNumber winningNumber = winningNumber(1201);
        when(lottoApiClient.fetch(1201)).thenReturn(Optional.of(winningNumber));
        when(persister.upsert(winningNumber)).thenReturn(UpsertOutcome.INSERTED);
        when(winningNumberRepository.findMaxRound()).thenReturn(Optional.of(1201));

        var response = collector.collectOne(1201, false);

        assertThat(response.collected()).isEqualTo(1);
        assertThat(response.dataChanged()).isTrue();
        assertSavedLog(LottoFetchStatus.SUCCESS, 1201, null, null);
    }

    @Test
    @DisplayName("추첨되지 않은 회차 응답을 기록하고 실패로 간주하지 않는다")
    void recordsNotDrawnResponse() {
        LottoSingleDrawCollector collector = collector();
        when(lottoApiClient.fetch(1202)).thenReturn(Optional.empty());
        when(winningNumberRepository.findMaxRound()).thenReturn(Optional.of(1201));

        var response = collector.collectOne(1202, false);

        assertThat(response.notDrawn()).isTrue();
        assertThat(response.failed()).isZero();
        assertThat(response.latestRound()).isEqualTo(1201);
        verify(persister, never()).upsert(org.mockito.ArgumentMatchers.any());
        assertSavedLog(LottoFetchStatus.NOT_DRAWN, 1202, null, null);
    }

    @Test
    @DisplayName("외부 API 실패를 실패 회차로 변환하고 응답 상세 정보를 보존한다")
    void recordsApiFailure() {
        LottoSingleDrawCollector collector = collector();
        when(lottoApiClient.fetch(1203))
                .thenThrow(new LottoApiClientException("bad gateway", 502, "upstream body"));
        when(winningNumberRepository.findMaxRound()).thenReturn(Optional.of(1201));

        var response = collector.collectOne(1203, false);

        assertThat(response.failed()).isEqualTo(1);
        assertThat(response.failedRounds()).containsExactly(1203);
        assertThat(response.latestRound()).isEqualTo(1201);
        assertSavedLog(LottoFetchStatus.FAILED, 1203, 502, "upstream body");
    }

    private LottoSingleDrawCollector collector() {
        return new LottoSingleDrawCollector(
                lottoApiClient,
                winningNumberRepository,
                persister,
                fetchLogRepository,
                CLOCK
        );
    }

    private void assertSavedLog(LottoFetchStatus status,
                                int round,
                                Integer responseCode,
                                String rawResponse) {
        ArgumentCaptor<LottoFetchLogEntity> captor = ArgumentCaptor.forClass(LottoFetchLogEntity.class);
        verify(fetchLogRepository).save(captor.capture());
        LottoFetchLogEntity log = captor.getValue();
        assertThat(log.getDrwNo()).isEqualTo(round);
        assertThat(log.getWinningRound()).isEqualTo(status == LottoFetchStatus.SUCCESS ? round : null);
        assertThat(log.getStatus()).isEqualTo(status);
        assertThat(log.getResponseCode()).isEqualTo(responseCode);
        assertThat(log.getRawResponse()).isEqualTo(rawResponse);
        assertThat(log.getFetchedAt()).isEqualTo(LocalDateTime.ofInstant(CLOCK.instant(), CLOCK.getZone()));
    }
}
