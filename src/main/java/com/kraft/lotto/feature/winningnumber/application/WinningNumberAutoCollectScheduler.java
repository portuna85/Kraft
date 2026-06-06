package com.kraft.lotto.feature.winningnumber.application;

import com.kraft.lotto.feature.winningnumber.web.dto.CollectResponse;
import com.kraft.lotto.support.LogSanitizer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
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
    private static final String COLLECT_ALL_LOCK_NAME = "collect-all";

    private final LottoCollectionCommandService collectionService;
    private final MeterRegistry meterRegistry;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Autowired
    public WinningNumberAutoCollectScheduler(LottoCollectionCommandService collectionService,
                                             ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this.collectionService = collectionService;
        this.meterRegistry = meterRegistryProvider.getIfAvailable(SimpleMeterRegistry::new);
    }

    @PostConstruct
    void preRegisterMetrics() {
        List.of("sat-22-30", "sat-23-30", "sun-07-00", "mon-10-10").forEach(trigger ->
                List.of("success", "failure").forEach(status ->
                        Counter.builder("kraft.collect.auto.run")
                                .tags("trigger", trigger, "status", status)
                                .register(meterRegistry)));
    }

    @Scheduled(
            cron = "${kraft.collect.auto.cron.saturday-22-30:0 30 22 ? * SAT}",
            zone = "${kraft.collect.auto.zone:Asia/Seoul}"
    )
    @SchedulerLock(name = COLLECT_ALL_LOCK_NAME, lockAtMostFor = "PT3M", lockAtLeastFor = "PT30S")
    public void collectSaturday2230() {
        runCollectAll("sat-22-30");
    }

    @Scheduled(
            cron = "${kraft.collect.auto.cron.saturday-23-30:0 30 23 ? * SAT}",
            zone = "${kraft.collect.auto.zone:Asia/Seoul}"
    )
    @SchedulerLock(name = COLLECT_ALL_LOCK_NAME, lockAtMostFor = "PT3M", lockAtLeastFor = "PT30S")
    public void collectSaturday2330() {
        runCollectAll("sat-23-30");
    }

    @Scheduled(
            cron = "${kraft.collect.auto.cron.sunday-07-00:0 0 7 ? * SUN}",
            zone = "${kraft.collect.auto.zone:Asia/Seoul}"
    )
    @SchedulerLock(name = COLLECT_ALL_LOCK_NAME, lockAtMostFor = "PT3M", lockAtLeastFor = "PT30S")
    public void collectSunday0700() {
        runCollectAll("sun-07-00");
    }

    @Scheduled(
            cron = "${kraft.collect.auto.cron.monday-10-10:0 10 10 ? * MON}",
            zone = "${kraft.collect.auto.zone:Asia/Seoul}"
    )
    @SchedulerLock(name = COLLECT_ALL_LOCK_NAME, lockAtMostFor = "PT3M", lockAtLeastFor = "PT30S")
    public void collectMonday1010() {
        runCollectAll("mon-10-10");
    }

    private void runCollectAll(String trigger) {
        // ShedLock prevents concurrent runs across nodes; AtomicBoolean guards within a single node
        // in case the scheduler fires twice before the lock is released (e.g. clock skew).
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
                    trigger, classifyException(ex), LogSanitizer.sanitizeLogValue(ex.getMessage()));
            log.error("lotto collect-all fail  trigger={}", trigger, ex);
        } finally {
            recordRun(trigger, status, startedAt);
            running.set(false);
        }
    }

    private void recordRun(String trigger, String status, long startedAt) {
        meterRegistry.counter("kraft.collect.auto.run", "trigger", trigger, "status", status).increment();
        meterRegistry.timer("kraft.collect.auto.latency", "trigger", trigger, "status", status)
                .record(Duration.ofNanos(System.nanoTime() - startedAt));
    }

    private void recordRoundFailures(String trigger, int failedRounds) {
        if (failedRounds <= 0) {
            return;
        }
        meterRegistry.counter("kraft.collect.auto.round.failure", "trigger", trigger).increment(failedRounds);
    }

    private void recordFailure(String trigger, Exception ex) {
        meterRegistry.counter(
                "kraft.collect.auto.error",
                "trigger", trigger,
                "exception", classifyException(ex)
        ).increment();
    }

    private void recordOverlapSkip(String trigger) {
        meterRegistry.counter("kraft.collect.auto.run", "trigger", trigger, "status", "skipped_overlap").increment();
    }

    private static String classifyException(Exception ex) {
        if (ex == null) {
            return "unknown";
        }
        String simpleName = ex.getClass().getSimpleName().toLowerCase();
        return simpleName.isBlank() ? "unknown" : simpleName;
    }

}
