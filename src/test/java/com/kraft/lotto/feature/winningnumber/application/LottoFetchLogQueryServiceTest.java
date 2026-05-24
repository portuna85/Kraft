package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.kraft.lotto.feature.winningnumber.infrastructure.LottoFetchLogRepository;
import com.kraft.lotto.feature.winningnumber.web.dto.FetchLogRetentionStatusDto;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

    @Test
    @DisplayName("retention 상태 응답에 설정/집계/컷오프를 포함한다")
    void returnsRetentionStatusSnapshot() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-05-24T12:00:00Z"), ZoneId.of("UTC"));
        LottoFetchLogQueryService service = new LottoFetchLogQueryService(fetchLogRepository, fixedClock);
        LocalDateTime cutoff = LocalDateTime.of(2026, 2, 23, 12, 0);

        when(fetchLogRepository.count()).thenReturn(123L);
        when(fetchLogRepository.countByFetchedAtBefore(cutoff)).thenReturn(9L);
        when(fetchLogRepository.findOldestFetchedAt()).thenReturn(LocalDateTime.of(2025, 1, 1, 0, 0));
        when(fetchLogRepository.findNewestFetchedAt()).thenReturn(LocalDateTime.of(2026, 5, 24, 11, 59));

        FetchLogRetentionStatusDto result = service.retentionStatus(
                true,
                90,
                1000,
                "0 30 3 * * *",
                "Asia/Seoul"
        );

        assertThat(result.enabled()).isTrue();
        assertThat(result.retentionDays()).isEqualTo(90);
        assertThat(result.deleteBatchSize()).isEqualTo(1000);
        assertThat(result.cutoff()).isEqualTo(cutoff);
        assertThat(result.totalLogs()).isEqualTo(123L);
        assertThat(result.purgeEligibleLogs()).isEqualTo(9L);
        assertThat(result.oldestFetchedAt()).isEqualTo(LocalDateTime.of(2025, 1, 1, 0, 0));
        assertThat(result.newestFetchedAt()).isEqualTo(LocalDateTime.of(2026, 5, 24, 11, 59));
    }
}

