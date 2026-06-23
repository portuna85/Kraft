package com.kraft.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "email_subscriptions")
public class EmailSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_token_hash", nullable = false, length = 64, unique = true)
    private String deviceTokenHash;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(nullable = false)
    private boolean verified;

    @Column(name = "verification_token", nullable = false, length = 36, unique = true)
    private String verificationToken;

    @Column(name = "unsubscribe_token", nullable = false, length = 36, unique = true)
    private String unsubscribeToken;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected EmailSubscription() {
    }

    public EmailSubscription(String deviceTokenHash, String email, String verificationToken,
                             String unsubscribeToken, OffsetDateTime createdAt) {
        this.deviceTokenHash = deviceTokenHash;
        this.email = email;
        this.verified = false;
        this.verificationToken = verificationToken;
        this.unsubscribeToken = unsubscribeToken;
        this.createdAt = createdAt;
    }

    public void verify(OffsetDateTime now) {
        this.verified = true;
        this.verifiedAt = now;
    }

    public void updateEmail(String newEmail, String newVerificationToken) {
        this.email = newEmail;
        this.verified = false;
        this.verificationToken = newVerificationToken;
        this.verifiedAt = null;
    }

    public Long getId() { return id; }
    public String getDeviceTokenHash() { return deviceTokenHash; }
    public String getEmail() { return email; }
    public boolean isVerified() { return verified; }
    public String getVerificationToken() { return verificationToken; }
    public String getUnsubscribeToken() { return unsubscribeToken; }
    public OffsetDateTime getVerifiedAt() { return verifiedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
