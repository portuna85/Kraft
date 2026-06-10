package com.kraft.lotto.feature.push.domain;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "device_tokens")
@SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "JPA entity — mutable by design")
public class DeviceTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token", nullable = false, unique = true, length = 255)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 10)
    private Platform platform;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    protected DeviceTokenEntity() {
    }

    public DeviceTokenEntity(String token, Platform platform, LocalDateTime now) {
        this.token = token;
        this.platform = platform;
        this.registeredAt = now;
        this.lastSeenAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public Platform getPlatform() {
        return platform;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void refreshLastSeen(LocalDateTime now) {
        this.lastSeenAt = now;
    }
}
