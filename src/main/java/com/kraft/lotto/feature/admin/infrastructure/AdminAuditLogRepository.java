package com.kraft.lotto.feature.admin.infrastructure;

import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;

public interface AdminAuditLogRepository
        extends JpaRepository<AdminAuditLogEntity, Long>,
                JpaSpecificationExecutor<AdminAuditLogEntity> {

    Page<AdminAuditLogEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Modifying
    int deleteByCreatedAtBefore(LocalDateTime cutoff);
}
