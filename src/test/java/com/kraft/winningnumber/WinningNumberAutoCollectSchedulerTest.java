package com.kraft.winningnumber;

import com.kraft.common.config.ExternalLottoProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("로또 자동 수집 스케줄러 — gap 기반 catch-up 상한 테스트")
class WinningNumberAutoCollectSchedulerTest {

    @Mock
    private WinningNumberCollectionService collectionService;
    @Mock
    private LottoFreshnessMetrics freshnessMetrics;

    private WinningNumberAutoCollectScheduler scheduler;

    @Test
    @DisplayName("4주 뒤처졌으면(gap=4) catch-up 상한이 5회로 계산된다")
    void collectLatestAutomatically_fourWeekGap_requestsFiveRounds() {
        ExternalLottoProperties properties = new ExternalLottoProperties("https://example.com/{round}", null, null, null);
        scheduler = new WinningNumberAutoCollectScheduler(properties, collectionService, freshnessMetrics);
        given(freshnessMetrics.snapshot()).willReturn(new LottoFreshnessMetrics.FreshnessSnapshot(1196, 1200, 28));
        given(collectionService.collectUpToLatest(5)).willReturn(java.util.List.of());

        scheduler.collectLatestAutomatically();

        verify(collectionService).collectUpToLatest(5);
    }

    @Test
    @DisplayName("이미 최신이면(gap<=0) catch-up 상한이 1로 클램프된다")
    void collectLatestAutomatically_alreadyUpToDate_clampsToOne() {
        ExternalLottoProperties properties = new ExternalLottoProperties("https://example.com/{round}", null, null, null);
        scheduler = new WinningNumberAutoCollectScheduler(properties, collectionService, freshnessMetrics);
        given(freshnessMetrics.snapshot()).willReturn(new LottoFreshnessMetrics.FreshnessSnapshot(1200, 1200, 0));
        given(collectionService.collectUpToLatest(1)).willReturn(java.util.List.of());

        scheduler.collectLatestAutomatically();

        ArgumentCaptor<Integer> maxRoundsCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(collectionService).collectUpToLatest(maxRoundsCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(maxRoundsCaptor.getValue()).isEqualTo(1);
    }

    @Test
    @DisplayName("외부 수집 URL이 설정되지 않으면 수집을 건너뛴다")
    void collectLatestAutomatically_disabled_skipsCollection() {
        ExternalLottoProperties properties = new ExternalLottoProperties("", null, null, null);
        scheduler = new WinningNumberAutoCollectScheduler(properties, collectionService, freshnessMetrics);

        scheduler.collectLatestAutomatically();

        org.mockito.Mockito.verifyNoInteractions(collectionService, freshnessMetrics);
    }
}
