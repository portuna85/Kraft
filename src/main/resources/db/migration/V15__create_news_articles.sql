CREATE TABLE news_articles (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    title        VARCHAR(500)  NOT NULL,
    link         VARCHAR(2000) NOT NULL,
    link_hash    VARCHAR(64)   NOT NULL,
    description  TEXT,
    source       VARCHAR(200),
    pub_date     DATETIME,
    collected_at DATETIME      NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_news_link_hash (link_hash),
    INDEX idx_news_pub_date     (pub_date),
    INDEX idx_news_collected_at (collected_at)
);
