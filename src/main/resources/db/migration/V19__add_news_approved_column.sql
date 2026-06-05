ALTER TABLE news_articles
    ADD COLUMN approved BOOLEAN NOT NULL DEFAULT TRUE;

-- 기존 GENERAL 등급 기사를 미승인 상태로 초기화
UPDATE news_articles SET approved = FALSE WHERE source_tier = 'GENERAL';

CREATE INDEX idx_news_approved_pub_date
    ON news_articles (approved, pub_date);
