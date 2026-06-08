package com.kraft.lotto.feature.admin.application;

import com.kraft.lotto.feature.admin.infrastructure.AdminAuditLogRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AdminAuditLogRetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(AdminAuditLogRetentionScheduler.class);

    private final AdminAuditLogRepository repository;
    private final Clock clock;

    @Value("${KRAFT_ADMIN_AUDIT_RETENTION_DAYS:365}")
    private int retentionDays;

    @Scheduled(cron = "0 0 4 * * *")
    @SchedulerLock(name = "adminAuditLogRetention", lockAtMostFor = "PT10M")
    @Transactional
    public void purgeOldLogs() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusDays(retentionDays);
        int deleted = repository.deleteByCreatedAtBefore(cutoff);
        if (deleted > 0) {
            log.info("admin audit log retention: deleted={} cutoff={}", deleted, cutoff);
        }
    }
}
