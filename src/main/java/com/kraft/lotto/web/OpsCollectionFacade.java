package com.kraft.lotto.web;

import com.kraft.lotto.feature.winningnumber.application.LottoCollectionCommandService;
import com.kraft.lotto.feature.winningnumber.web.dto.CollectResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class OpsCollectionFacade {

    private static final Duration MANUAL_LOCK_MAX = Duration.ofMinutes(10);

    private final LottoCollectionCommandService collectionCommandService;
    private final LockingTaskExecutor lockingTaskExecutor;
    private final Clock clock;

    public OpsCollectionFacade(LottoCollectionCommandService collectionCommandService,
                               LockingTaskExecutor lockingTaskExecutor,
                               Clock clock) {
        this.collectionCommandService = collectionCommandService;
        this.lockingTaskExecutor = lockingTaskExecutor;
        this.clock = clock;
    }

    public CollectResponse collectLatest() {
        return withLock("collect-all", collectionCommandService::collectAllUntilLatest);
    }

    public CollectResponse collectMissing() {
        return withLock("ops-collect-missing", collectionCommandService::collectMissingOnce);
    }

    private CollectResponse withLock(String lockName, java.util.function.Supplier<CollectResponse> action) {
        try {
            var result = lockingTaskExecutor.executeWithLock(
                    action::get,
                    new LockConfiguration(Instant.now(clock), lockName, MANUAL_LOCK_MAX, Duration.ZERO));
            return result.wasExecuted() ? result.getResult() : CollectResponse.ofOverlapSkipped(0);
        } catch (Throwable e) {
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (e instanceof Error error) {
                throw error;
            }
            throw new CollectionLockException("collection lock failed", (Exception) e);
        }
    }
}
