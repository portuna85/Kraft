package com.kraft.admin;

import java.time.OffsetDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
    Page<AdminAuditLog> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);
    Page<AdminAuditLog> findByAdminUserOrderByCreatedAtDescIdDesc(String adminUser, Pageable pageable);

    // 파생 delete는 대상 행을 전부 로드한 뒤 건별로 삭제한다 — 벌크 DELETE 한 번으로 대체한다.
    @Modifying
    @Query("delete from AdminAuditLog a where a.createdAt < :cutoff")
    int deleteByCreatedAtBefore(@Param("cutoff") OffsetDateTime cutoff);
}
