package com.kraft.lotto.feature.admin.application;

import com.kraft.lotto.feature.admin.infrastructure.AdminAuditLogEntity;
import com.kraft.lotto.feature.admin.infrastructure.AdminAuditLogRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
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

    @Transactional
    public void recordFailure(String actor, String action, String target,
                              String ip, String ua, String errorMessage) {
        save(actor, action, target, ip, ua, "FAILURE", errorMessage);
    }

    @Transactional(readOnly = true)
    public Page<AdminAuditLogEntity> list(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable);
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
}
