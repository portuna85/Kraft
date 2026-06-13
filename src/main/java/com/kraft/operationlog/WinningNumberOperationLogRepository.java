package com.kraft.operationlog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface WinningNumberOperationLogRepository extends JpaRepository<WinningNumberOperationLog, Long>,
        JpaSpecificationExecutor<WinningNumberOperationLog> {

    Page<WinningNumberOperationLog> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);
}
