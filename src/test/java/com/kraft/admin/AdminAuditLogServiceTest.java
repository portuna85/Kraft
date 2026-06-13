package com.kraft.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("관리자 감사 로그 서비스 테스트")
class AdminAuditLogServiceTest {

    @Autowired
    private AdminAuditLogService service;

    @Autowired
    private AdminAuditLogRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("로그 기록이 올바르게 저장되는지 확인")
    void record_savesEntry() {
        service.record("admin1", "LOGIN_SUCCESS", null, null, "10.0.0.1");

        Page<AdminAuditLog> page = service.findAll(PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(1);
        AdminAuditLog log = page.getContent().get(0);
        assertThat(log.getAdminUser()).isEqualTo("admin1");
        assertThat(log.getAction()).isEqualTo("LOGIN_SUCCESS");
        assertThat(log.getIpAddress()).isEqualTo("10.0.0.1");
        assertThat(log.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("전체 로그 조회가 생성일시 내림차순으로 반환되는지 확인")
    void findAll_returnsAllEntriesOrderedByCreatedAtDesc() {
        service.record("admin1", "ACTION_A", null, null, "1.1.1.1");
        service.record("admin2", "ACTION_B", null, null, "2.2.2.2");

        Page<AdminAuditLog> page = service.findAll(PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(2);
        // most recent first
        assertThat(page.getContent().get(0).getAdminUser()).isEqualTo("admin2");
    }

    @Test
    @DisplayName("사용자별 로그 조회가 올바르게 작동하는지 확인")
    void findByUser_returnsOnlyMatchingUser() {
        service.record("alice", "LOGIN_SUCCESS", null, null, "1.1.1.1");
        service.record("bob", "LOGIN_FAILURE", null, null, "2.2.2.2");
        service.record("alice", "COLLECT_LATEST", "round=1200", null, "1.1.1.1");

        Page<AdminAuditLog> alicePage = service.findByUser("alice", PageRequest.of(0, 10));
        assertThat(alicePage.getTotalElements()).isEqualTo(2);
        assertThat(alicePage.getContent()).allMatch(l -> l.getAdminUser().equals("alice"));

        Page<AdminAuditLog> bobPage = service.findByUser("bob", PageRequest.of(0, 10));
        assertThat(bobPage.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("로그 기록 시 대상과 상세 내용이 올바르게 저장되는지 확인")
    void record_savesTargetAndDetail() {
        service.record("admin", "COLLECT_ROUND", "round=100", "detail text", "3.3.3.3");

        Page<AdminAuditLog> page = service.findAll(PageRequest.of(0, 10));
        AdminAuditLog log = page.getContent().get(0);
        assertThat(log.getTarget()).isEqualTo("round=100");
        assertThat(log.getDetail()).isEqualTo("detail text");
    }
}
