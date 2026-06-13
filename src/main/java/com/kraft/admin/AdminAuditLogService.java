package com.kraft.admin;

import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuditLogService {

    private final AdminAuditLogRepository repo;
    private final Clock clock;

    public AdminAuditLogService(AdminAuditLogRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    @Transactional
    public void record(String adminUser, String action, String target, String detail, String ip) {
        repo.save(new AdminAuditLog(adminUser, action, target, detail, ip,
                OffsetDateTime.now(clock)));
    }

    @Transactional(readOnly = true)
    public Page<AdminAuditLog> findAll(Pageable pageable) {
        return repo.findAllByOrderByCreatedAtDescIdDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Page<AdminAuditLog> findByUser(String adminUser, Pageable pageable) {
        return repo.findByAdminUserOrderByCreatedAtDescIdDesc(adminUser, pageable);
    }
}
