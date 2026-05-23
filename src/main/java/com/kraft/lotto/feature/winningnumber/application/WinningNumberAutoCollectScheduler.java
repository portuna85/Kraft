package com.kraft.lotto.feature.winningnumber.application;

import com.kraft.lotto.feature.winningnumber.web.dto.CollectResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression("${kraft.lotto.scheduler.enabled:true} and ${kraft.collect.auto.enabled:true}")
@SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "Spring-managed scheduler constructor validates required wiring")
public class WinningNumberAutoCollectScheduler {

    private static final Logger log = LoggerFactory.getLogger(WinningNumberAutoCollectScheduler.class);

    private final LottoCollectionCommandService collectionService;
    private final MeterRegistry meterRegistry;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Autowired
    public WinningNumberAutoCollectScheduler(LottoCollectionCommandService collectionService,
                                             ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this.collectionService = collectionService;
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
    }

    @Scheduled(
            cron = "${kraft.collect.auto.cron.saturday-22-30:0 30 22 ? * SAT}",
            zone = "${kraft.collect.auto.zone:Asia/Seoul}"
    )
    @SchedulerLock(name = "collect-all-sat-22-30", lockAtMostFor = "PT3M", lockAtLeastFor = "PT30S")
    public void collectSaturday2230() {
        runCollectAll("sat-22-30");
    }

    @Scheduled(
            cron = "${kraft.collect.auto.cron.sunday-07-00:0 0 7 ? * SUN}",
            zone = "${kraft.collect.auto.zone:Asia/Seoul}"
    )
    @SchedulerLock(name = "collect-all-sun-07-00", lockAtMostFor = "PT3M", lockAtLeastFor = "PT30S")
    public void collectSunday0700() {
        runCollectAll("sun-07-00");
    }

    private void runCollectAll(String trigger) {
        if (!running.compareAndSet(false, true)) {
            log.warn("lotto collect-all skipped trigger={} reason=overlap", trigger);
            recordOverlapSkip(trigger);
            return;
        }
        long startedAt = System.nanoTime();
        String status = "success";
        try {
            log.info("lotto collect-all start trigger={}", trigger);
            CollectResponse r = collectionService.collectAllUntilLatest();
            log.info("lotto collect-all done  trigger={} collected={} updated={} skipped={} failed={} latestRound={}",
                    trigger, r.collected(), r.updated(), r.skipped(), r.failed(), r.latestRound());
            recordRoundFailures(trigger, r.failed());
        } catch (Exception ex) {
            status = "failure";
            recordFailure(trigger, ex);
            log.error("[ALERT] lotto collect-all fail trigger={} exception={} message={}",
                    trigger, classifyException(ex), sanitizeLogValue(ex.getMessage()));
            log.error("lotto collect-all fail  trigger={}", trigger, ex);
        } finally {
            recordRun(trigger, status, startedAt);
            running.set(false);
        }
    }

    private void recordRun(String trigger, String status, long startedAt) {
        if (meterRegistry == null) {
            return;
        }
        meterRegistry.counter("kraft.collect.auto.run", "trigger", trigger, "status", status).increment();
        meterRegistry.timer("kraft.collect.auto.latency", "trigger", trigger, "status", status)
                .record(Duration.ofNanos(System.nanoTime() - startedAt));
    }

    private void recordRoundFailures(String trigger, int failedRounds) {
        if (meterRegistry == null || failedRounds <= 0) {
            return;
        }
        meterRegistry.counter("kraft.collect.auto.round.failure", "trigger", trigger).increment(failedRounds);
    }

    private void recordFailure(String trigger, Exception ex) {
        if (meterRegistry == null) {
            return;
        }
        meterRegistry.counter(
                "kraft.collect.auto.error",
                "trigger", trigger,
                "exception", classifyException(ex)
        ).increment();
    }

    private void recordOverlapSkip(String trigger) {
        if (meterRegistry == null) {
            return;
        }
        meterRegistry.counter("kraft.collect.auto.run", "trigger", trigger, "status", "skipped_overlap").increment();
    }

    private static String classifyException(Exception ex) {
        if (ex == null) {
            return "unknown";
        }
        String simpleName = ex.getClass().getSimpleName().toLowerCase();
        return simpleName.isBlank() ? "unknown" : simpleName;
    }

    private static String sanitizeLogValue(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replace('\r', '_').replace('\n', '_');
    }
}
