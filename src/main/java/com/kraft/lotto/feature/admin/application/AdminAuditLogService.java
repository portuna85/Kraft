package com.kraft.lotto.feature.admin.application;

import com.kraft.lotto.feature.admin.infrastructure.AdminAuditLogEntity;
import com.kraft.lotto.feature.admin.infrastructure.AdminAuditLogRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAuditLogService {

    private final AdminAuditLogRepository repository;
    private final Clock clock;

    @Transactional
    public void recordSuccess(String actor, String action, String target, String ip, String ua) {
        save(actor, action, target, ip, ua, "SUCCESS", null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String actor, String action, String target,
                              String ip, String ua, String errorMessage) {
        save(actor, action, target, ip, ua, "FAILURE", errorMessage);
    }

    @Transactional(readOnly = true)
    public Page<AdminAuditLogEntity> list(AuditFilter filter, Pageable pageable) {
        if (!filter.hasFilters()) {
            return repository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return repository.findAll(toSpec(filter), pageable);
    }

    private static Specification<AdminAuditLogEntity> toSpec(AuditFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.action() != null && !filter.action().isBlank()) {
                predicates.add(cb.like(cb.upper(root.get("action")),
                        "%" + filter.action().toUpperCase() + "%"));
            }
            if (filter.result() != null && !filter.result().isBlank()) {
                predicates.add(cb.equal(root.get("result"), filter.result()));
            }
            if (filter.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"),
                        filter.from().atStartOfDay()));
            }
            if (filter.to() != null) {
                predicates.add(cb.lessThan(root.get("createdAt"),
                        filter.to().plusDays(1).atStartOfDay()));
            }
            query.orderBy(cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void save(String actor, String action, String target,
                      String ip, String ua, String result, String errorMessage) {
        repository.save(AdminAuditLogEntity.builder()
                .actor(actor)
                .action(action)
                .target(target)
                .requestIp(ip)
                .userAgent(ua)
                .result(result)
                .errorMessage(errorMessage)
                .createdAt(LocalDateTime.now(clock))
                .build());
    }

    public record AuditFilter(String action, String result, LocalDate from, LocalDate to) {

        public static AuditFilter empty() {
            return new AuditFilter(null, null, null, null);
        }

        public boolean hasFilters() {
            return (action != null && !action.isBlank())
                    || (result != null && !result.isBlank())
                    || from != null
                    || to != null;
        }
    }
}
