package com.kraft.lotto.feature.admin.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AdminAuditLogRepository
        extends JpaRepository<AdminAuditLogEntity, Long>,
                JpaSpecificationExecutor<AdminAuditLogEntity> {

    Page<AdminAuditLogEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
