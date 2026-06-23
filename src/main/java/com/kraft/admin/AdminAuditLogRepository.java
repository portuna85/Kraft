package com.kraft.admin;

import java.time.OffsetDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
    Page<AdminAuditLog> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);
    Page<AdminAuditLog> findByAdminUserOrderByCreatedAtDescIdDesc(String adminUser, Pageable pageable);
    void deleteByCreatedAtBefore(OffsetDateTime cutoff);
}
