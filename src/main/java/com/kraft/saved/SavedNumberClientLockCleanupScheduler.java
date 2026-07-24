package com.kraft.saved;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.OffsetDateTime;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 저장번호를 모두 지운 뒤에도 saved_number_client_locks에 영구히 남는 고아 잠금 행을
 * 주기적으로 삭제한다(P1-06) — LogRetentionScheduler와 동일한 패턴.
 */
@Component
public class SavedNumberClientLockCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(SavedNumberClientLockCleanupScheduler.class);

    private final SavedNumberClientLockRepository repository;
    private final Clock clock;

    @Value("${kraft.retention.saved-number-lock-orphan-days:30}")
    private int orphanRetentionDays;

    public SavedNumberClientLockCleanupScheduler(SavedNumberClientLockRepository repository,
                                                  Clock clock,
                                                  MeterRegistry meterRegistry) {
        this.repository = repository;
        this.clock = clock;
        Gauge.builder("kraft_saved_number_client_locks_total", repository, r -> (double) r.count())
                .description("saved_number_client_locks 테이블의 전체 행 수(고아 행 포함, 무한 증가 여부 관찰용)")
                .register(meterRegistry);
    }

    // LogRetentionScheduler의 03:00과 겹치지 않도록 03:30에 실행한다.
    @Scheduled(cron = "0 30 3 * * *", zone = "Asia/Seoul")
    @SchedulerLock(name = "purge-orphan-saved-number-locks", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    @Transactional
    public void purgeOrphanLocks() {
        OffsetDateTime cutoff = OffsetDateTime.now(clock).minusDays(orphanRetentionDays);
        int deleted = repository.deleteOrphansOlderThan(cutoff);
        log.info("고아 저장번호 잠금 행 삭제 완료: cutoff={} deleted={}", cutoff, deleted);
    }
}
