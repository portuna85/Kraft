CREATE TABLE email_subscriptions (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    device_token_hash   VARCHAR(64)  NOT NULL,
    email               VARCHAR(254) NOT NULL,
    verified            BOOLEAN      NOT NULL DEFAULT FALSE,
    verification_token  VARCHAR(36)  NOT NULL,
    unsubscribe_token   VARCHAR(36)  NOT NULL,
    verified_at         DATETIME(6)  NULL,
    created_at          DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_email_sub_device (device_token_hash),
    UNIQUE KEY uk_email_sub_verify_token (verification_token),
    UNIQUE KEY uk_email_sub_unsub_token (unsubscribe_token),
    KEY idx_email_sub_verified (verified, created_at)
);
