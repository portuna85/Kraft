package com.kraft.operationlog;

import com.kraft.common.web.RequestIdFilter;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import org.slf4j.MDC;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WinningNumberOperationLogService {

    private final WinningNumberOperationLogRepository repository;
    private final Clock clock;

    public WinningNumberOperationLogService(WinningNumberOperationLogRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSuccess(WinningNumberOperationType operationType,
                           Integer round,
                           String sourceDetail,
                           String message) {
        repository.save(new WinningNumberOperationLog(
                operationType,
                WinningNumberOperationStatus.SUCCESS,
                round,
                truncate(sourceDetail, 255),
                truncate(message, 500),
                truncate(MDC.get(RequestIdFilter.MDC_KEY), 100),
                OffsetDateTime.now(clock)
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailure(WinningNumberOperationType operationType,
                           Integer round,
                           String sourceDetail,
                           String message) {
        repository.save(new WinningNumberOperationLog(
                operationType,
                WinningNumberOperationStatus.FAILURE,
                round,
                truncate(sourceDetail, 255),
                truncate(message, 500),
                truncate(MDC.get(RequestIdFilter.MDC_KEY), 100),
                OffsetDateTime.now(clock)
        ));
    }

    @Transactional(readOnly = true)
    public WinningNumberOperationLogPageResponse getRecentLogs(int page, int size, WinningNumberOperationLogFilter filter) {
        var pageable = PageRequest.of(page, size, Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")
        ));
        var result = repository.findAll((root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<Predicate>();
            if (filter.operationType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("operationType"), filter.operationType()));
            }
            if (filter.executionStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("executionStatus"), filter.executionStatus()));
            }
            if (filter.round() != null) {
                predicates.add(criteriaBuilder.equal(root.get("round"), filter.round()));
            }
            if (filter.createdFrom() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), filter.createdFrom()));
            }
            if (filter.createdToExclusive() != null) {
                predicates.add(criteriaBuilder.lessThan(root.get("createdAt"), filter.createdToExclusive()));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        }, pageable);
        return new WinningNumberOperationLogPageResponse(
                result.getContent().stream().map(WinningNumberOperationLogResponse::from).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
