package com.kraft.lotto.feature.winningnumber.application;

import com.kraft.lotto.feature.winningnumber.domain.WinningNumber;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.util.concurrent.TimeUnit;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 당첨번호 저장 책임을 캡슐화한다.
 *
 * <p>회차별 독립 트랜잭션으로 저장해 수집 루프에서 특정 회차 저장 실패가
 * 다른 회차 처리 결과까지 롤백하지 않도록 한다.</p>
 */
@Component
public class WinningNumberPersister {
    private static final int UPSERT_MAX_RETRIES_ON_OPTIMISTIC_LOCK = 2;

    private final WinningNumberUpsertExecutor executor;
    private final MeterRegistry meterRegistry;

    @Autowired
    public WinningNumberPersister(WinningNumberUpsertExecutor executor,
                                  ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this(executor, meterRegistryProvider.getIfAvailable(SimpleMeterRegistry::new));
    }

    WinningNumberPersister(WinningNumberUpsertExecutor executor, MeterRegistry meterRegistry) {
        this.executor = executor;
        this.meterRegistry = meterRegistry;
    }

    public UpsertOutcome upsert(WinningNumber winningNumber) {
        long started = System.nanoTime();
        UpsertOutcome outcome = UpsertOutcome.FAILED;
        for (int attempt = 1; attempt <= UPSERT_MAX_RETRIES_ON_OPTIMISTIC_LOCK; attempt++) {
            try {
                outcome = executor.upsertOnce(winningNumber);
                break;
            } catch (DataIntegrityViolationException ex) {
                outcome = UpsertOutcome.UNCHANGED;
                if (attempt < UPSERT_MAX_RETRIES_ON_OPTIMISTIC_LOCK) {
                    continue;
                }
                break;
            } catch (OptimisticLockingFailureException ex) {
                if (attempt == UPSERT_MAX_RETRIES_ON_OPTIMISTIC_LOCK) {
                    meterRegistry.counter("kraft.winningnumber.optimistic_lock.failure").increment();
                    outcome = UpsertOutcome.FAILED;
                }
            }
        }
        recordDbSaveLatency(started, switch (outcome) {
            case INSERTED -> "insert";
            case UPDATED -> "update";
            case UNCHANGED -> "unchanged";
            case FAILED -> "failed";
        });
        return outcome;
    }

    private void recordDbSaveLatency(long started, String mode) {
        meterRegistry.timer("kraft.winningnumber.db.save.latency", "mode", mode)
                .record(System.nanoTime() - started, TimeUnit.NANOSECONDS);
    }
}
