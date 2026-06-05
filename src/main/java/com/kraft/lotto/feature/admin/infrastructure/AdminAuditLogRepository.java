package com.kraft.lotto.feature.admin.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLogEntity, Long> {

    Page<AdminAuditLogEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
