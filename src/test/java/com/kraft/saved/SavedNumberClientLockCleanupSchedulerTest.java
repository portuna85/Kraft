package com.kraft.saved;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("저장번호 고아 잠금 정리 스케줄러 단위 테스트")
class SavedNumberClientLockCleanupSchedulerTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Instant NOW = Instant.parse("2026-07-17T00:00:00Z");

    @Mock
    private SavedNumberClientLockRepository repository;

    @Test
    @DisplayName("설정된 보관기간만큼 뒤로 계산한 cutoff로 고아 행 삭제를 호출한다")
    void purgeOrphanLocks_deletesUsingConfiguredRetentionCutoff() {
        Clock clock = Clock.fixed(NOW, KST);
        given(repository.deleteOrphansOlderThan(any())).willReturn(3);

        SavedNumberClientLockCleanupScheduler scheduler =
                new SavedNumberClientLockCleanupScheduler(repository, clock, new SimpleMeterRegistry());
        ReflectionTestUtils.setField(scheduler, "orphanRetentionDays", 30);

        scheduler.purgeOrphanLocks();

        ArgumentCaptor<OffsetDateTime> cutoffCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(repository).deleteOrphansOlderThan(cutoffCaptor.capture());
        assertThat(cutoffCaptor.getValue()).isEqualTo(OffsetDateTime.now(clock).minusDays(30));
    }

    @Test
    @DisplayName("kraft_saved_number_client_locks_total gauge는 repository count를 그대로 반영한다")
    void locksTotalGauge_reflectsRepositoryCount() {
        Clock clock = Clock.fixed(NOW, KST);
        given(repository.count()).willReturn(42L);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        new SavedNumberClientLockCleanupScheduler(repository, clock, meterRegistry);

        assertThat(meterRegistry.get("kraft_saved_number_client_locks_total").gauge().value()).isEqualTo(42d);
    }
}
