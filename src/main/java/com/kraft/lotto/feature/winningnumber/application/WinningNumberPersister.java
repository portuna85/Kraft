package com.kraft.lotto.feature.winningnumber.application;

import com.kraft.lotto.feature.winningnumber.domain.WinningNumber;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WinningNumberPersister {

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
        UpsertOutcome outcome = executor.upsertOnce(winningNumber);
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
