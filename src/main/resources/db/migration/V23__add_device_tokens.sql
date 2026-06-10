CREATE TABLE IF NOT EXISTS device_tokens (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    token         VARCHAR(255) NOT NULL,
    platform      VARCHAR(10)  NOT NULL,
    registered_at DATETIME     NOT NULL,
    last_seen_at  DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_device_tokens_token (token),
    INDEX idx_device_tokens_last_seen (last_seen_at)
);
