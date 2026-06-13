package com.kraft.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "admin_audit_log")
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String adminUser;

    @Column(nullable = false, length = 200)
    private String action;

    @Column(length = 200)
    private String target;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(length = 45)
    private String ipAddress;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    protected AdminAuditLog() {}

    public AdminAuditLog(String adminUser, String action, String target, String detail,
                         String ipAddress, OffsetDateTime createdAt) {
        this.adminUser = adminUser;
        this.action = action;
        this.target = target;
        this.detail = detail;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getAdminUser() { return adminUser; }
    public String getAction() { return action; }
    public String getTarget() { return target; }
    public String getDetail() { return detail; }
    public String getIpAddress() { return ipAddress; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
