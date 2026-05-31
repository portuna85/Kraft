package com.kraft.lotto.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.kraft.lotto.feature.winningnumber.application.LottoCollectionCommandService;
import com.kraft.lotto.feature.winningnumber.web.dto.CollectResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpsCollectionFacade")
class OpsCollectionFacadeTest {

    @Mock
    LottoCollectionCommandService commandService;

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    private static final CollectResponse SAMPLE =
            CollectResponse.of(3, 0, 0, 1100, List.of(), false, null, false);

    /** 항상 락 획득에 성공하는 executor */
    private static LockingTaskExecutor acquiringExecutor() {
        SimpleLock lock = () -> { };
        LockProvider provider = config -> Optional.of(lock);
        return new DefaultLockingTaskExecutor(provider);
    }

    /** 항상 락 획득에 실패하는 executor (overlap skip) */
    private static LockingTaskExecutor nonAcquiringExecutor() {
        LockProvider provider = config -> Optional.empty();
        return new DefaultLockingTaskExecutor(provider);
    }

    @Test
    @DisplayName("collectLatest: 락 획득 성공 시 실행 결과를 반환한다")
    void collectLatestExecutesWhenLockAcquired() {
        when(commandService.collectAllUntilLatest()).thenReturn(SAMPLE);
        OpsCollectionFacade facade = new OpsCollectionFacade(commandService, acquiringExecutor(), FIXED_CLOCK);

        CollectResponse result = facade.collectLatest("req-1", "127.0.0.1");

        assertThat(result.collected()).isEqualTo(3);
    }

    @Test
    @DisplayName("collectLatest: 락 획득 실패(overlap) 시 collected=0을 반환한다")
    void collectLatestSkipsWhenLockNotAcquired() {
        OpsCollectionFacade facade = new OpsCollectionFacade(commandService, nonAcquiringExecutor(), FIXED_CLOCK);

        CollectResponse result = facade.collectLatest("req-2", "127.0.0.1");

        assertThat(result.collected()).isEqualTo(0);
    }

    @Test
    @DisplayName("collectMissing: 락 획득 성공 시 실행 결과를 반환한다")
    void collectMissingExecutesWhenLockAcquired() {
        when(commandService.collectMissingOnce()).thenReturn(SAMPLE);
        OpsCollectionFacade facade = new OpsCollectionFacade(commandService, acquiringExecutor(), FIXED_CLOCK);

        CollectResponse result = facade.collectMissing("req-3", "127.0.0.1");

        assertThat(result.collected()).isEqualTo(3);
    }

    @Test
    @DisplayName("락 실행 중 RuntimeException은 그대로 re-throw된다")
    void runtimeExceptionIsRethrown() {
        when(commandService.collectAllUntilLatest()).thenThrow(new RuntimeException("db connection lost"));
        OpsCollectionFacade facade = new OpsCollectionFacade(commandService, acquiringExecutor(), FIXED_CLOCK);

        assertThatThrownBy(() -> facade.collectLatest("req-4", "127.0.0.1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db connection lost");
    }

    @Test
    @DisplayName("null requestId/clientIp는 빈 문자열로 대체되어 예외 없이 처리된다")
    void nullAuditParamsAreSanitized() {
        OpsCollectionFacade facade = new OpsCollectionFacade(commandService, nonAcquiringExecutor(), FIXED_CLOCK);

        CollectResponse result = facade.collectLatest(null, null);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("체크드 예외는 CollectionLockException으로 래핑된다")
    void checkedExceptionIsWrapped() {
        LockProvider provider = config -> { throw new RuntimeException("shedlock failure"); };
        LockingTaskExecutor executor = new DefaultLockingTaskExecutor(provider);
        OpsCollectionFacade facade = new OpsCollectionFacade(commandService, executor, FIXED_CLOCK);

        assertThatThrownBy(() -> facade.collectLatest("req-5", "127.0.0.1"))
                .isInstanceOf(RuntimeException.class);
    }
}
