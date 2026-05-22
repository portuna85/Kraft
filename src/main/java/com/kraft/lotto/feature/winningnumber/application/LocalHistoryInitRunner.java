package com.kraft.lotto.feature.winningnumber.application;

import com.kraft.lotto.feature.winningnumber.web.dto.CollectResponse;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "kraft.history-init.enabled", havingValue = "true")
public class LocalHistoryInitRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalHistoryInitRunner.class);

    private final LottoCollectionCommandService collectionService;
    private final WinningNumberQueryService queryService;
    private final AsyncTaskExecutor taskExecutor;
    private volatile Future<?> runningTask;

    public LocalHistoryInitRunner(LottoCollectionCommandService collectionService,
                                  WinningNumberQueryService queryService,
                                  @Qualifier("applicationTaskExecutor") AsyncTaskExecutor taskExecutor) {
        this.collectionService = collectionService;
        this.queryService = queryService;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (queryService.findLatest().isEmpty()) {
            log.info("history-init: DB is empty, starting background full-history collection...");
            submit("full-history", collectionService::collectAllHistory);
        } else {
            log.info("history-init: DB has data, syncing latest rounds...");
            submit("latest-sync", collectionService::collectAllUntilLatest);
        }
    }

    @PreDestroy
    public void shutdown() {
        Future<?> task = runningTask;
        if (task != null && !task.isDone()) {
            task.cancel(true);
            log.info("history-init task cancellation requested");
        }
    }

    private void submit(String mode, Supplier<CollectResponse> action) {
        Future<?> task = runningTask;
        if (task != null && !task.isDone()) {
            log.warn("history-init: {} skipped because a previous task is still running", mode);
            return;
        }
        runningTask = taskExecutor.submit(() -> runTask(mode, action));
    }

    private void runTask(String mode, Supplier<CollectResponse> action) {
        try {
            CollectResponse r = action.get();
            log.info("history-init {} done: collected={} updated={} skipped={} failed={} latestRound={}",
                    mode, r.collected(), r.updated(), r.skipped(), r.failed(), r.latestRound());
        } catch (RuntimeException ex) {
            log.error("history-init {} failed", mode, ex);
        }
    }
}
