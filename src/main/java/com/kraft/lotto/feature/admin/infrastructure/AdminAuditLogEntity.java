package com.kraft.lotto.feature.admin.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "admin_audit_log")
@Getter
@NoArgsConstructor
public class AdminAuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String actor;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(length = 500)
    private String target;

    @Column(name = "request_ip", length = 100)
    private String requestIp;

    @Column(name = "user_agent", length = 1000)
    private String userAgent;

    @Column(nullable = false, length = 50)
    private String result;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public AdminAuditLogEntity(String actor, String action, String target,
                               String requestIp, String userAgent,
                               String result, String errorMessage,
                               LocalDateTime createdAt) {
        this.actor = actor;
        this.action = action;
        this.target = target;
        this.requestIp = requestIp;
        this.userAgent = userAgent;
        this.result = result;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
    }
}
