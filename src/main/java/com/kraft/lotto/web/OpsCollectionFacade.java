package com.kraft.lotto.web;

import com.kraft.lotto.feature.winningnumber.application.LottoCollectionCommandService;
import com.kraft.lotto.feature.winningnumber.web.dto.CollectResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OpsCollectionFacade {

    private static final Logger log = LoggerFactory.getLogger(OpsCollectionFacade.class);
    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("kraft.audit");

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

    public CollectResponse collectLatest(String requestId, String clientIp) {
        return withLock("collect-all", "collect-latest", requestId, clientIp,
                collectionCommandService::collectAllUntilLatest);
    }

    public CollectResponse collectMissing(String requestId, String clientIp) {
        return withLock("ops-collect-missing", "collect-missing", requestId, clientIp,
                collectionCommandService::collectMissingOnce);
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\r', ' ').replace('\n', ' ');
    }

    private CollectResponse withLock(String lockName, String action,
                                     String requestId, String clientIp,
                                     java.util.function.Supplier<CollectResponse> task) {
        String safeIp = sanitize(clientIp);
        String safeRequestId = sanitize(requestId);
        try {
            var result = lockingTaskExecutor.executeWithLock(
                    task::get,
                    new LockConfiguration(Instant.now(clock), lockName, MANUAL_LOCK_MAX, Duration.ZERO));
            CollectResponse response = result.wasExecuted()
                    ? result.getResult()
                    : CollectResponse.ofOverlapSkipped(0);
            String outcome = result.wasExecuted() ? "executed" : "skipped";
            AUDIT_LOG.info("action={} outcome={} requestId={} clientIp={} collected={}",
                    action, outcome, safeRequestId, safeIp,
                    result.wasExecuted() ? response.collected() : 0);
            return response;
        } catch (Throwable e) {
            AUDIT_LOG.warn("action={} outcome=error requestId={} clientIp={} error={}",
                    action, safeRequestId, safeIp, sanitize(e.getMessage()));
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
