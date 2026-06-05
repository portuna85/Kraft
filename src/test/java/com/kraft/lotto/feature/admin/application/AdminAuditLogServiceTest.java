package com.kraft.lotto.feature.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.verify;

import com.kraft.lotto.feature.admin.infrastructure.AdminAuditLogEntity;
import com.kraft.lotto.feature.admin.infrastructure.AdminAuditLogRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAuditLogService")
class AdminAuditLogServiceTest {

    @Mock
    AdminAuditLogRepository repository;

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-06-05T12:00:00Z"), ZoneId.of("Asia/Seoul"));

    AdminAuditLogService service;

    @BeforeEach
    void setUp() {
        service = new AdminAuditLogService(repository, CLOCK);
    }

    @Test
    @DisplayName("recordSuccess — result=SUCCESS, errorMessage=null로 저장한다")
    void recordSuccessSavesCorrectFields() {
        service.recordSuccess("admin@example.com", "COLLECT_LATEST", "round:1230",
                "127.0.0.1", "Mozilla/5.0");

        ArgumentCaptor<AdminAuditLogEntity> captor = forClass(AdminAuditLogEntity.class);
        verify(repository).save(captor.capture());
        AdminAuditLogEntity saved = captor.getValue();

        assertThat(saved.getActor()).isEqualTo("admin@example.com");
        assertThat(saved.getAction()).isEqualTo("COLLECT_LATEST");
        assertThat(saved.getTarget()).isEqualTo("round:1230");
        assertThat(saved.getResult()).isEqualTo("SUCCESS");
        assertThat(saved.getErrorMessage()).isNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("recordFailure — result=FAILURE, errorMessage 저장한다")
    void recordFailureSavesErrorMessage() {
        service.recordFailure("admin@example.com", "NEWS_APPROVE", "articleId:42",
                "10.0.0.1", "curl/7.x", "article not found");

        ArgumentCaptor<AdminAuditLogEntity> captor = forClass(AdminAuditLogEntity.class);
        verify(repository).save(captor.capture());
        AdminAuditLogEntity saved = captor.getValue();

        assertThat(saved.getResult()).isEqualTo("FAILURE");
        assertThat(saved.getErrorMessage()).isEqualTo("article not found");
        assertThat(saved.getRequestIp()).isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("list — repository.findAllByOrderByCreatedAtDesc를 위임 호출한다")
    void listDelegatesToRepository() {
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(0, 10);
        service.list(pageable);
        verify(repository).findAllByOrderByCreatedAtDesc(pageable);
    }
}
