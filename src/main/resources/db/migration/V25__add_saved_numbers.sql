CREATE TABLE IF NOT EXISTS saved_numbers (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    device_token VARCHAR(255) NOT NULL,
    numbers      VARCHAR(50)  NOT NULL,
    label        VARCHAR(100) NULL,
    saved_at     DATETIME     NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_saved_numbers_token (device_token),
    INDEX idx_saved_numbers_token_saved (device_token, saved_at DESC)
);
