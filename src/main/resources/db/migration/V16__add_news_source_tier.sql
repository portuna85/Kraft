ALTER TABLE news_articles
    ADD COLUMN source_tier VARCHAR(20) NOT NULL DEFAULT 'GENERAL';

CREATE INDEX idx_news_source_tier_pub_date
    ON news_articles (source_tier, pub_date);
