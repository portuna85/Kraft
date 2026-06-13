package com.kraft.admin;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuditLogService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final AdminAuditLogRepository repo;

    public AdminAuditLogService(AdminAuditLogRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public void record(String adminUser, String action, String target, String detail, String ip) {
        repo.save(new AdminAuditLog(adminUser, action, target, detail, ip,
                OffsetDateTime.now(KST)));
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
