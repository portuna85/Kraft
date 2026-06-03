package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.kraft.lotto.feature.winningnumber.infrastructure.LottoFetchLogEntity;
import com.kraft.lotto.feature.winningnumber.infrastructure.LottoFetchLogRepository;
import com.kraft.lotto.feature.winningnumber.infrastructure.LottoFetchStatus;
import com.kraft.lotto.feature.winningnumber.web.dto.DataChangeLogDto;
import com.kraft.lotto.feature.winningnumber.web.dto.FetchLogRetentionStatusDto;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("수집 실패 로그 조회 서비스 테스트")
class LottoFetchLogQueryServiceTest {

    @Mock
    LottoFetchLogRepository fetchLogRepository;

    private LottoFetchLogQueryService service() {
        return new LottoFetchLogQueryService(fetchLogRepository,
                Clock.fixed(Instant.parse("2026-05-24T12:00:00Z"), ZoneId.of("UTC")));
    }

    @Test
    @DisplayName("retention 상태 응답에 설정/집계/컷오프를 포함한다")
    void returnsRetentionStatusSnapshot() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-05-24T12:00:00Z"), ZoneId.of("UTC"));
        LottoFetchLogQueryService svc = new LottoFetchLogQueryService(fetchLogRepository, fixedClock);
        LocalDateTime cutoff = LocalDateTime.of(2026, 2, 23, 12, 0);

        when(fetchLogRepository.count()).thenReturn(123L);
        when(fetchLogRepository.countByFetchedAtBefore(cutoff)).thenReturn(9L);
        when(fetchLogRepository.findOldestFetchedAt()).thenReturn(LocalDateTime.of(2025, 1, 1, 0, 0));
        when(fetchLogRepository.findNewestFetchedAt()).thenReturn(LocalDateTime.of(2026, 5, 24, 11, 59));

        FetchLogRetentionStatusDto result = svc.retentionStatus(true, 90, 1000, "0 30 3 * * *", "Asia/Seoul");

        assertThat(result.enabled()).isTrue();
        assertThat(result.retentionDays()).isEqualTo(90);
        assertThat(result.cutoff()).isEqualTo(cutoff);
        assertThat(result.totalLogs()).isEqualTo(123L);
        assertThat(result.purgeEligibleLogs()).isEqualTo(9L);
    }

    @Test
    @DisplayName("reason 파라미터가 null이면 필터 없이 조회한다")
    void nullReasonPassesNullFilter() {
        lenient().when(fetchLogRepository.findRecentFailedFilteredByReason(isNull(), isNull(), isNull(), any()))
                .thenReturn(List.of());

        var result = service().failureReasonsResponse(100, null, null, null);

        assertThat(result.items()).isEmpty();
        assertThat(result.reason()).isNull();
    }

    @Test
    @DisplayName("reason 파라미터가 공백이면 null로 정규화된다")
    void blankReasonNormalizesToNull() {
        lenient().when(fetchLogRepository.findRecentFailedFilteredByReason(isNull(), isNull(), isNull(), any()))
                .thenReturn(List.of());

        var result = service().failureReasonsResponse(100, "   ", null, null);

        assertThat(result.reason()).isEqualTo("   ");
    }

    @Test
    @DisplayName("reason 파라미터가 있으면 소문자 변환 후 필터로 전달된다")
    void nonBlankReasonPassedAsLowercase() {
        lenient().when(fetchLogRepository.findRecentFailedFilteredByReason(isNull(), isNull(), any(), any()))
                .thenReturn(List.of());

        var result = service().failureReasonsResponse(100, "TIMEOUT", null, null);

        assertThat(result.items()).isEmpty();
    }

    @Test
    @DisplayName("failuresResponse는 빈 저장소에서 빈 items DTO를 반환한다")
    void failuresResponseReturnsDtoWithEmptyItems() {
        lenient().when(fetchLogRepository.findRecentFailedFilteredByReason(isNull(), isNull(), isNull(), any()))
                .thenReturn(List.of());

        var result = service().failuresResponse(100, null, null, null);

        assertThat(result.items()).isNotNull();
        assertThat(result.limit()).isEqualTo(100);
    }

    @Test
    @DisplayName("failureOverview는 reason·log 집계를 함께 반환한다")
    void failureOverviewCombinesBothSummaries() {
        lenient().when(fetchLogRepository.findRecentFailedFilteredByReason(isNull(), isNull(), isNull(), any()))
                .thenReturn(List.of());

        var result = service().failureOverview(200, 100, null, null, null);

        assertThat(result.reasons()).isNotNull();
        assertThat(result.recentFailures()).isNotNull();
    }

    @Test
    @DisplayName("PagedFailures: null rows는 빈 리스트로 대체된다")
    void pagedFailuresNullRowsFallsBack() {
        var paged = new LottoFetchLogQueryService.PagedFailures(null, 1, 20, false);

        assertThat(paged.rows()).isEmpty();
    }

    @Test
    @DisplayName("recentCollectionLogs는 수집 이력을 DataChangeLogDto 목록으로 반환한다")
    void recentCollectionLogsReturnsDtos() {
        LottoFetchLogEntity entity = new LottoFetchLogEntity(
                1100, null, LottoFetchStatus.SUCCESS, null, 200, null,
                LocalDateTime.of(2026, 6, 1, 22, 30));
        when(fetchLogRepository.findRecentAll(any())).thenReturn(List.of(entity));

        List<DataChangeLogDto> result = service().recentCollectionLogs(10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).round()).isEqualTo(1100);
        assertThat(result.get(0).status()).isEqualTo(LottoFetchStatus.SUCCESS);
        assertThat(result.get(0).statusLabel()).isEqualTo("수집 성공");
    }

    @Test
    @DisplayName("recentCollectionLogs limit은 1~50으로 제한된다")
    void recentCollectionLogsClampLimit() {
        when(fetchLogRepository.findRecentAll(any())).thenReturn(List.of());

        assertThat(service().recentCollectionLogs(0)).isEmpty();
        assertThat(service().recentCollectionLogs(999)).isEmpty();
    }
}
