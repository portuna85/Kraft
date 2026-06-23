package com.kraft.operationlog;

import com.kraft.admin.AdminAuditLogRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Component
public class LogRetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(LogRetentionScheduler.class);

    private final WinningNumberOperationLogRepository operationLogRepository;
    private final AdminAuditLogRepository adminAuditLogRepository;

    @Value("${kraft.retention.operation-log-days:30}")
    private int operationLogRetentionDays;

    @Value("${kraft.retention.admin-audit-log-days:90}")
    private int adminAuditLogRetentionDays;

    public LogRetentionScheduler(WinningNumberOperationLogRepository operationLogRepository,
                                 AdminAuditLogRepository adminAuditLogRepository) {
        this.operationLogRepository = operationLogRepository;
        this.adminAuditLogRepository = adminAuditLogRepository;
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    @SchedulerLock(name = "purge-old-logs", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    @Transactional
    public void purgeOldLogs() {
        OffsetDateTime operationLogCutoff = OffsetDateTime.now().minusDays(operationLogRetentionDays);
        operationLogRepository.deleteByCreatedAtBefore(operationLogCutoff);
        log.info("작업 로그 보관기간 초과 행 삭제 완료: cutoff={}", operationLogCutoff);

        OffsetDateTime adminAuditLogCutoff = OffsetDateTime.now().minusDays(adminAuditLogRetentionDays);
        adminAuditLogRepository.deleteByCreatedAtBefore(adminAuditLogCutoff);
        log.info("관리자 감사 로그 보관기간 초과 행 삭제 완료: cutoff={}", adminAuditLogCutoff);
    }
}
