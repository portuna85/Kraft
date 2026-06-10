package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kraft.lotto.feature.winningnumber.web.dto.CollectResponse;
import com.kraft.lotto.feature.winningnumber.web.dto.WinningNumberDto;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.task.AsyncTaskExecutor;

@ExtendWith(MockitoExtension.class)
@DisplayName("로컬 히스토리 초기화 실행기 테스트")
class LocalHistoryInitRunnerTest {

    @Mock
    LottoCollectionCommandService collectionService;

    @Mock
    WinningNumberQueryService queryService;

    @Mock
    ApplicationArguments args;

    @Test
    @DisplayName("빈 데이터베이스에서 전체 히스토리 수집을 시작한다")
    void emptyDbStartsFullHistoryCollection() {
        when(queryService.findLatest()).thenReturn(Optional.empty());
        when(collectionService.collectAllHistory()).thenReturn(CollectResponse.ofInserted(1, 1));

        new LocalHistoryInitRunner(collectionService, queryService, Runnable::run).run(args);

        verify(collectionService).collectAllHistory();
        verify(collectionService, never()).collectAllUntilLatest();
    }

    @Test
    @DisplayName("비어 있지 않은 데이터베이스에서 최신 회차 동기화를 시작한다")
    void nonEmptyDbStartsLatestSync() {
        when(queryService.findLatest()).thenReturn(Optional.of(latest()));
        when(collectionService.collectAllUntilLatest()).thenReturn(CollectResponse.ofSkipped(1, 1));

        new LocalHistoryInitRunner(collectionService, queryService, Runnable::run).run(args);

        verify(collectionService).collectAllUntilLatest();
        verify(collectionService, never()).collectAllHistory();
    }

    @Test
    @DisplayName("백그라운드 작업 실패 시 예외를 전파하지 않는다")
    void taskFailureDoesNotPropagateFromRunner() {
        when(queryService.findLatest()).thenReturn(Optional.empty());
        when(collectionService.collectAllHistory()).thenThrow(new IllegalStateException("boom"));
        LocalHistoryInitRunner runner = new LocalHistoryInitRunner(collectionService, queryService, Runnable::run);

        assertThatCode(() -> runner.run(args)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("종료 시 진행 중인 히스토리 작업을 취소한다")
    void shutdownCancelsOutstandingTask() {
        HoldingExecutor executor = new HoldingExecutor();
        when(queryService.findLatest()).thenReturn(Optional.empty());
        LocalHistoryInitRunner runner = new LocalHistoryInitRunner(collectionService, queryService, executor);

        runner.run(args);
        runner.shutdown();

        assertThat(executor.task).isNotNull();
        assertThat(executor.task.isCancelled()).isTrue();
    }

    private static WinningNumberDto latest() {
        return new WinningNumberDto(1, LocalDate.of(2002, 12, 7), List.of(1, 2, 3, 4, 5, 6),
                7, 0L, 0, 0L, 0L, 0, null);
    }

    private static class HoldingExecutor implements AsyncTaskExecutor {
        private FutureTask<?> task;

        @Override
        public void execute(Runnable task) {
            this.task = new FutureTask<>(task, null);
        }

        @Override
        public Future<?> submit(Runnable task) {
            this.task = new FutureTask<>(task, null);
            return this.task;
        }
    }
}
