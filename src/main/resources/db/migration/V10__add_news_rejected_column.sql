ALTER TABLE news_articles
    ADD COLUMN rejected BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_news_approved_rejected
    ON news_articles (approved, rejected);
