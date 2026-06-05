CREATE TABLE IF NOT EXISTS admin_audit_log (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    actor         VARCHAR(255)  NOT NULL,
    action        VARCHAR(100)  NOT NULL,
    target        VARCHAR(500)  NULL,
    request_ip    VARCHAR(100)  NULL,
    user_agent    VARCHAR(1000) NULL,
    result        VARCHAR(50)   NOT NULL,
    error_message VARCHAR(2000) NULL,
    created_at    DATETIME      NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_admin_audit_created_at (created_at),
    INDEX idx_admin_audit_action     (action),
    INDEX idx_admin_audit_actor      (actor)
);

CREATE TABLE IF NOT EXISTS news_blocked_domain (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    domain     VARCHAR(255) NOT NULL,
    reason     VARCHAR(500) NULL,
    created_by VARCHAR(255) NOT NULL,
    created_at DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_news_blocked_domain (domain)
);

CREATE TABLE IF NOT EXISTS news_blocked_keyword (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    keyword    VARCHAR(255) NOT NULL,
    reason     VARCHAR(500) NULL,
    created_by VARCHAR(255) NOT NULL,
    created_at DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_news_blocked_keyword (keyword)
);
