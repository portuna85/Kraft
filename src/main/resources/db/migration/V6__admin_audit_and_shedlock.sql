CREATE TABLE admin_audit_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    admin_user  VARCHAR(100) NOT NULL,
    action      VARCHAR(200) NOT NULL,
    target      VARCHAR(200) NULL,
    detail      TEXT         NULL,
    ip_address  VARCHAR(45)  NULL,
    created_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_audit_user_created (admin_user, created_at DESC),
    KEY idx_audit_created (created_at DESC)
);

CREATE TABLE shedlock (
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP(3) NOT NULL,
    locked_at  TIMESTAMP(3) NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
