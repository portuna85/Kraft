package com.kraft.operationlog;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WinningNumberOperationLogRepository extends JpaRepository<WinningNumberOperationLog, Long>,
        JpaSpecificationExecutor<WinningNumberOperationLog> {

    Page<WinningNumberOperationLog> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    // 공개 상태 페이지용: 수집 실패 또는 수동 보정만 "주목할 만한" 이력으로 본다.
    // 정상 자동 수집(EXTERNAL_COLLECT/SUCCESS)은 매주 발생하는 정상 동작이라 제외한다.
    @Query("SELECT l FROM WinningNumberOperationLog l "
            + "WHERE l.createdAt >= :since "
            + "AND (l.executionStatus = com.kraft.operationlog.WinningNumberOperationStatus.FAILURE "
            + "     OR l.operationType = com.kraft.operationlog.WinningNumberOperationType.MANUAL_UPSERT) "
            + "ORDER BY l.createdAt DESC")
    List<WinningNumberOperationLog> findNotableSince(@Param("since") OffsetDateTime since);

    @Query("SELECT COUNT(l) > 0 FROM WinningNumberOperationLog l "
            + "WHERE l.round = :round AND l.createdAt > :after "
            + "AND l.executionStatus = com.kraft.operationlog.WinningNumberOperationStatus.SUCCESS")
    boolean existsSuccessForRoundAfter(@Param("round") Integer round, @Param("after") OffsetDateTime after);

    void deleteByCreatedAtBefore(OffsetDateTime cutoff);
}
